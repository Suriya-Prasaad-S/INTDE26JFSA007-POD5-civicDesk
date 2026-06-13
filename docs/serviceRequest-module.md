# Service Request Management module — how it works

This is the developer guide for the **Service Request** module (CivicDesk Module 2.3).
It covers what every class does, how a request flows through the layers, the database
tables and their relationships, how to run and test it, and the one-step handoff for when
the IAM / Citizen teams deliver their real modules.

Everything lives under `src/main/java/com/civicdesk/module/serviceRequest/`.

---

## 1. The big picture

The module exposes three POST endpoints under `/civicDesk/serviceRequest` and is built in
the standard Spring layered style: **Controller → Service → Repository → Database**.

```
HTTP request
   │
   ▼
ServiceRequestController        ← REST layer. Validates the JSON/form body, returns 201.
   │   calls
   ▼
*Service classes                ← Business rules (the "what should happen" logic).
   │   calls
   ▼
*Repository interfaces          ← Spring Data JPA. Generates SQL for you. No hand SQL.
   │
   ▼
MySQL tables                    ← Created automatically by Hibernate from the @Entity classes.
```

There is **no `schema.sql` / `data.sql`**. Hibernate creates every table from the entity
classes (`spring.jpa.hibernate.ddl-auto=update`), and `DummyDataSeeder` inserts the
starter rows in Java on startup.

---

## 2. Package-by-package

| Package | What's in it | Responsibility |
|---------|--------------|----------------|
| `controller/` | `ServiceRequestController` | Maps the HTTP endpoints to service calls. Validates request bodies with `@Valid`. |
| `dto/request/` | `CreateServiceRequest`, `UpdateServiceRequest`, `SubmitServiceRequest`, `UpdateRequestStatusRequest`, `VerifyDocumentRequest` | Immutable Java `record`s describing the **input** JSON, with validation annotations. |
| `dto/response/` | `MessageResponse` (writes); `ServiceListItemResponse`, `ServiceDetailResponse`, `RequestListItemResponse`, `CitizenRequestItemResponse`, `RequestDetailResponse`, `DocumentItemResponse` (reads) | Shape of the **output** JSON. Writes return a `MessageResponse`; reads return purpose-built records. |
| `service/` | `ServiceCatalogService`, `ServiceRequestService`, `DocumentService`, `OfficerAssignment`, `CitizenLookup`, `FileStorageService` | The business logic. See §4. |
| `entity/` | `ServiceCatalog`, `ServiceRequest`, `RequestDocument` | The module's own JPA tables. |
| `entity/enums/` | `ServiceCategory`, `ServiceStatus`, `RequestStatus`, `VerificationStatus` | Fixed value sets. `RequestStatus.isTerminal()` = Completed/Rejected. **The JSON API uses single-letter status codes** (see below); the DB still stores the full names. |
| `entity/external/` | `Department`, `User`, `CitizenProfile` | **TEMPORARY** placeholders for tables other teams own (see §6). |
| `repository/` | one interface per entity | Spring Data JPA repositories — you declare a method name, Spring writes the query. |
| `bootstrap/` | `DummyDataSeeder` | **TEMPORARY** startup seeder (replaces the old `data.sql`). |

Status codes in the JSON API (request bodies and the `?status=` filter accept the code **or**
the full name; responses always return the code):
- `ServiceStatus`: Active=`A`, Inactive=`I`
- `RequestStatus`: Submitted=`S`, UnderReview=`U`, PendingDocuments=`P`, Approved=`A`, Rejected=`R`, Completed=`C`
- `VerificationStatus`: Pending=`P`, Verified=`V`, Rejected=`R`
- (`ServiceCategory` is unchanged — full names: Certificate / Utility / Registration / Welfare.)

Cross-cutting (shared with the whole app, under `com/civicdesk/common/`):

- `common/exception/` — typed exceptions that map cleanly to HTTP codes:
  `BadRequestException`→400, `ForbiddenException`→403, `ResourceNotFoundException`→404,
  `ConflictException`→409, `UnprocessableEntityException`→422.
- `common/exception/GlobalExceptionHandler` — catches those (and validation errors) and
  turns them into a uniform `ApiError` JSON body. This is why you get clean error JSON
  instead of stack traces.
- `common/response/ApiError` — the error body shape: a single `message` field
  (`{ "message": "..." }`). The HTTP status code is on the response status line, not in the body.

---

## 3. The endpoints

All under `/civicDesk/serviceRequest`. Grouped into catalog services, requests, documents.

**Catalog services**

| Method & path | Body | Does |
|---------------|------|------|
| `GET /getAllServices?category=` | — | Lists **Active** services; optional `category` filter. |
| `GET /getService/{serviceId}` | — | Full details of one service (404 if missing). |
| `POST /createService` | `CreateServiceRequest` | Adds a service to the catalog (201). |
| `PUT /updateService/{serviceId}` | `UpdateServiceRequest` | Updates editable fields / deactivates (404 if missing). |

**Service requests**

| Method & path | Body | Does |
|---------------|------|------|
| `POST /submitRequest` | `SubmitServiceRequest` | Citizen submits a request (201). |
| `GET /getAllRequests?status=&departmentId=` | — | Request queue; optional `status` / `departmentId` filters. |
| `GET /getRequest/{requestId}` | — | Full details incl. uploaded documents (404 if missing). |
| `GET /getRequestsByCitizen/{citizenId}` | — | A citizen's own requests (tracker view). |
| `PUT /updateRequestStatus/{requestId}` | `UpdateRequestStatusRequest` | Workflow transition (422 if invalid/terminal). |

**Documents**

| Method & path | Body | Does |
|---------------|------|------|
| `POST /uploadDocument/{requestId}` | multipart: `documentType`, `file` | Uploads a PDF/JPG/PNG (201). |
| `GET /getDocuments/{requestId}` | — | Lists a request's documents + statuses (404 if request missing). |
| `PUT /verifyDocument/{docId}` | `VerifyDocumentRequest` | Officer marks Verified/Rejected; a rejection sends the request back to `PendingDocuments`. |

> **Authentication/roles are not enforced yet** — `SecurityConfig` and the Spring Security
> dependency are commented out, so endpoints are open and the role-based 401/403 responses in
> the API spec are deferred until the IAM module lands. All non-auth checks (404 / 422 / 400 /
> 409) are enforced now.

---

## 4. What each service does (the flows)

### createService → `ServiceCatalogService`
1. Reject duplicates: `existsByServiceNameIgnoreCase`.
2. Look up the `Department` by `departmentId` (404 if it doesn't exist — this is a real FK now).
3. Build a `ServiceCatalog`, set its `department`, serialize `requiredDocuments` to a JSON
   string, default `fee` to 0 and `status` to `Active`, save.

### submitRequest → `ServiceRequestService`
1. `CitizenLookup.loadSubmittableCitizen` — citizen profile must exist (404) and its linked
   user must not be `Flagged` (403).
2. Catalog service must exist (404) and be `Active` (422 if Inactive).
3. `OfficerAssignment.findLeastLoadedOfficer` — the least-busy active officer in the
   service's department (422 if none).
4. Build the `ServiceRequest`: snapshot the `fee` and compute `expectedCompletionDate`
   (`today + processingDays`) **now**, so later catalog edits don't change open requests;
   set status `Submitted`; save.

### uploadDocument → `DocumentService`
1. `documentType` required (400).
2. Request must exist (404) and not be terminal (422 for Completed/Rejected).
3. `FileStorageService.store` validates the extension (pdf/jpg/jpeg/png, else 400) and
   writes the file under `./uploads/<requestId>/`, naming it
   `<citizenId>_<requestId>_<uuid>.<ext>`. Only the **path** is stored in the DB.
4. Save a `RequestDocument` with `verificationStatus = Pending`.

### updateService → `ServiceCatalogService`
Load the service (404 if missing), overwrite `serviceName` / `processingDays` / `fee` /
`status`, save. `departmentId` and `category` are fixed at creation and not editable here.

### Reads (`getAllServices`, `getService`, `getAllRequests`, `getRequest`, `getRequestsByCitizen`, `getDocuments`)
Each loads via a repository finder and maps entities → response records:
- `getAllServices` returns only **Active** services (optional `category` filter).
- `getRequest` also pulls the request's documents via `findByRequest_RequestId` and nests them.
- `getDocuments` 404s if the request id doesn't exist, then lists its documents.
- Filters on `getAllRequests` use derived queries that walk the associations
  (`findByService_Department_DepartmentId`, `findByStatus`, …).

### updateRequestStatus → `ServiceRequestService` (the workflow state machine)
1. Load the request (404 if missing).
2. If the current status is terminal (`Rejected`/`Completed`) → 422.
3. If `newStatus` isn't in `current.allowedNextStates()` → 422, listing the allowed states.
4. Otherwise set the new status and save. (`remarks` is accepted but not persisted — the ER
   schema has no column for it.)

The allowed transitions live on the `RequestStatus` enum (`allowedNextStates()`):
```
Submitted        → UnderReview
UnderReview      → PendingDocuments | Approved | Rejected
PendingDocuments → UnderReview | Rejected
Approved         → Completed | Rejected
Rejected, Completed → (terminal, none)
```

### verifyDocument → `DocumentService`
Validate `verificationStatus` is `Verified` or `Rejected` (else 400), load the document
(404 if missing), set its status. A **rejection** also moves the parent request to
`PendingDocuments` so the citizen can re-upload.

### Helpers
- **`OfficerAssignment`** — loads active officers via
  `UserRepository.findByRoleAndStatusAndDepartmentId("Officer","Active",deptId)`, then picks
  the one with the smallest `countByAssignedOfficerAndStatusNotIn(officer, [Completed,Rejected])`.
  (One small count query per officer — fine at this scale, and much easier to read than one
  big SQL statement.)
- **`CitizenLookup`** — loads the `CitizenProfile`, then its `User`, and rejects `Flagged`.
- **`FileStorageService`** — disk storage only; no DB.

---

## 5. Database tables & relationships

Tables use **snake_case** names; columns stay **camelCase** (set by
`PhysicalNamingStrategyStandardImpl` in `application.properties`). Every `FOREIGN KEY` in
the ER design is expressed as a JPA `@ManyToOne` with `@JoinColumn(name=...)`, so the FK
column keeps its original name and the API contract is unchanged.

```
departments ──┐
              │ (departmentId)
              ▼
        service_catalog ◄──────┐
              ▲                 │ (serviceId)
              │ (departmentId)  │
   users ─────┤                 │
     ▲        │ (assignedOfficerId)
     │        │                 │
     │   service_request ───────┘
     │        ▲   ▲
     │        │   │ (citizenId)
     │        │   └────────── citizen_profile
     │        │ (requestId)
     │   request_document
```

| Entity (this module) | Table | Association | FK column → table |
|----------------------|-------|-------------|-------------------|
| `ServiceCatalog` | `service_catalog` | `@ManyToOne department` | `departmentId` → `departments` |
| `ServiceRequest` | `service_request` | `@ManyToOne service` | `serviceId` → `service_catalog` |
| `ServiceRequest` | `service_request` | `@ManyToOne citizen` | `citizenId` → `citizen_profile` |
| `ServiceRequest` | `service_request` | `@ManyToOne assignedOfficer` | `assignedOfficerId` → `users` |
| `RequestDocument` | `request_document` | `@ManyToOne request` | `requestId` → `service_request` |

The responses are always DTOs (never the entities), so there's no lazy-loading / JSON
recursion problem from these associations.

---

## 6. Placeholder tables & the handoff (IMPORTANT)

`departments`, `users`, and `citizen_profile` are owned by the **IAM** and **Citizen**
teams, which aren't ready yet. So this module ships minimal placeholders in
`entity/external/` plus the `DummyDataSeeder`, purely so the foreign keys resolve and the
endpoints are testable today.

**When the real IAM / Citizen modules land:**
1. Delete the `entity/external/` package (`Department`, `User`, `CitizenProfile`) and
   `bootstrap/DummyDataSeeder`.
2. Repoint the five `@ManyToOne` imports (in `ServiceCatalog` and `ServiceRequest`, plus the
   `User` type used by `OfficerAssignment` / `CitizenLookup` / `ServiceRequestRepository`) at
   the real entities those teams provide.

Two `@Entity` classes mapped to the same table will crash Hibernate at startup, so this
deletion is required — it's the planned merge point, not optional cleanup.

---

## 7. Run & test

**Prerequisites:** MySQL running locally (`application.properties` points at
`jdbc:mysql://localhost:3306/civicdesk`, user/pass `root`/`root`), JDK 21.

```powershell
# from the project root
./mvnw.cmd spring-boot:run
```

On startup Hibernate creates the six tables and `DummyDataSeeder` inserts the dummy rows.

**Dummy data you can test with:**
- Departments: `dept-0004` (Citizen Services), `dept-0002` (Public Works)
- Officers: `off-0004`, `off-0006` (dept-0004), `off-0002` (dept-0002)
- Citizens: `citizen-0001` (Active), `citizen-0002` (Flagged)
- Catalog services: `svc-0001`, `svc-0002` (Active), `svc-0003` (Inactive)

**Smoke test (PowerShell `curl`/Invoke-RestMethod or Postman):**

```bash
# 1. Create a service (note: departmentId must already exist)
curl -X POST http://localhost:8082/civicDesk/serviceRequest/createService \
  -H "Content-Type: application/json" \
  -d '{"serviceName":"Marriage Certificate","departmentId":"dept-0004","category":"Certificate","processingDays":5,"requiredDocuments":["NationalID"],"fee":120.00}'
# → 201

# 2. Submit a request as an Active citizen for a seeded service → 201, officer auto-assigned
curl -X POST http://localhost:8082/civicDesk/serviceRequest/submitRequest \
  -H "Content-Type: application/json" \
  -d '{"citizenId":"citizen-0001","serviceId":"svc-0001"}'

# 3. Submit as the Flagged citizen → 403
curl -X POST http://localhost:8082/civicDesk/serviceRequest/submitRequest \
  -H "Content-Type: application/json" \
  -d '{"citizenId":"citizen-0002","serviceId":"svc-0001"}'

# 4. Upload a document (use a requestId from a service_request row)
curl -X POST http://localhost:8082/civicDesk/serviceRequest/uploadDocument/<requestId> \
  -F "documentType=NationalID" -F "file=@/path/to/file.pdf"
# → 201 for pdf/jpg/png, 400 for anything else
```

Swagger UI is available at `http://localhost:8082/swagger-ui.html` (springdoc is on the
classpath) for clicking through the endpoints.
