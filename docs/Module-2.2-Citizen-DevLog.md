# CivicDesk — Module 2.2: Citizen Profile & Registration

> ⚠️ **Historical (pre-2026-06-16 refactor).** Design rationale still holds, but specifics changed:
> IDs are now **16-char alphanumeric** (not UUID), schema is **snake_case**, status is **single-char**
> (`A/V/F`, `V/E/R`), the **issue-dummy endpoint was removed**, register returns **no id**, and real
> file storage + a supervisor-gated verify were added. Current truth:
> [`Module-2.2-CONTEXT.md`](./Module-2.2-CONTEXT.md) (§6) and
> [`Module-2.2-API-Reference.md`](./Module-2.2-API-Reference.md).

**Development Log & Design Rationale**

| | |
|---|---|
| **Module** | 2.2 — Citizen Profile & Registration (+ Citizen Documents) |
| **Owner** | Pruthiviraj |
| **Branch** | `feature/citizenProfile` |
| **Base path** | `/civicDesk/citizenProfile` |
| **API spec** | API Reference **v4** (verb-based paths) — supersedes v3 |
| **Stack** | Java 21, Spring Boot 3.4.1, Spring Data JPA, MySQL, Bean Validation |
| **Auth** | JWT from Module 2.1 (IAM) — **owned by a teammate, not implemented here** |
| **Last updated** | 2026-06-11 |

---

## 1. Purpose of this module

Manages the **citizen identity record** and its **supporting documents** for the CivicDesk
platform. It is one of six modules in a shared modular-monolith Spring Boot application; every
teammate owns one module folder under `com.civicdesk.module.*` and merges back into `main`.

This module exposes **10 endpoints** (see §6) covering registration, profile read/update,
admin status changes, document upload/issue/verify, and ward-based lookup.

---

## 2. Scope & boundaries (what this module does NOT do)

- **No JWT / authentication logic.** Module 2.1 (IAM) owns the JWT filter and security wiring.
  This module is built so that authentication can be layered on top later without changing the
  business code.
- **No changes to other modules** (`iam`, `servicerequest`, `permit`, `grievance`, `analytics`).
- **No changes to shared build/config** unless explicitly agreed with the team
  (`pom.xml`, shared `config/`, shared `common/`).

All work so far is confined to `src/main/java/com/civicdesk/module/citizen/`.

---

## 3. Key decisions & rationale

| Decision | Choice | Why |
|---|---|---|
| **API version** | v4 (verb-based paths, e.g. `/registerCitizen`, `/getProfile/{id}`) | v4 is the newer reference and supersedes v3's RESTful paths. |
| **Authentication** | Mocked / deferred — not implemented | IAM (Module 2.1) owns it. Building it here would duplicate a teammate's work and couple the modules. The data + business layers don't need it. |
| **`verifiedBy`** | Taken from the request body for now | The spec explicitly says it will auto-extract from the JWT once IAM integrates. |
| **Build order** | In tiers (data layer first, then logic, then auth) | Lets work proceed immediately without waiting on IAM, and isolates risk. See §4. |
| **PK format** | `CHAR(36)` UUID v4 | Project-wide convention across all six modules; non-guessable. |
| **Module coupling** | `CitizenDocument.citizenId` is a plain `String`, **not** a JPA relationship | Keeps this module decoupled — it does not import other modules' classes and compiles independently. |
| **DTOs vs entities** | Separate classes (records for DTOs) | Lets us mask sensitive fields (national ID), expose different fields per endpoint, and keep the API contract independent of the DB schema. |

### Tiered build plan

- **Tier 1 — Data foundation** *(✅ done — this log)*: entities, enums, repositories, DTOs.
  Pure to the module folder, zero dependency on JWT or shared files, zero merge-conflict risk.
- **Tier 2 — Business logic + API**: service classes, REST controllers, file storage, the shared
  `common/` response envelope + exception handler, and a **temporary** `permitAll` security
  config so the app runs locally. (Some of this touches shared `common/`/`config/` — needs a
  quick team agreement on ownership.)
- **Tier 3 — Auth integration** *(after Module 2.1 lands)*: real JWT security filter,
  "own profile only" ownership checks, role-based 403s, and switching `verifiedBy` to the token.

---

## 4. Architecture — layering

A standard Spring Boot layered design. Tier 1 implements the bottom three layers:

```
HTTP request
   │
   ▼
[ Controller ]   ← Tier 2   (REST endpoints, @Valid, returns the response envelope)
   │
   ▼
[ Service ]      ← Tier 2   (business rules: validation, status transitions, file limits)
   │
   ▼
[ Repository ]   ← Tier 1 ✅ (Spring Data — DB access, no SQL written)
   │
   ▼
[ Entity ]       ← Tier 1 ✅ (maps to MySQL tables)

[ DTO ]          ← Tier 1 ✅ (request/response JSON shapes, used by controller & service)
[ Enum ]         ← Tier 1 ✅ (allowed value sets, enforced in Java + DB CHECK)
```

**Why bottom-up:** the data model is the hardest thing to change once other layers (and other
teammates' modules) depend on it, so it is settled first.

---

## 5. Tier 1 — what was built, file by file

All under `com.civicdesk.module.citizen`.

### 5.1 Enums — `entity/`
Lock allowed values at the Java layer (and mirrored by DB CHECK constraints on the entities).

| File | Values |
|---|---|
| `Gender` | Male, Female, Other |
| `CitizenStatus` | Active, Verified, Flagged |
| `DocumentStatus` | Valid, Expired, Revoked |
| `DocumentType` | NationalID, ResidenceProof, BirthCertificate, IncomeCertificate |

**Why:** a typo like `"Mael"` can never be stored — invalid values are impossible by construction.

### 5.2 Entities — `entity/`
JPA classes that become MySQL tables on startup (`spring.jpa.hibernate.ddl-auto=update`).

**`CitizenProfile`** → table `citizenProfile`
- `citizenId` (PK, CHAR(36)), `name`, `dateOfBirth`, `gender`, `nationalIdNumber` (unique),
  `address`, `ward`, `zone`, `email` (unique), `phone`, `status`, `createdAt`, `updatedAt`.
- `gender` and `status` carry `@Check` constraints so the DB rejects bad values too.
- `createdAt`/`updatedAt` auto-populated by Hibernate (`@CreationTimestamp` / `@UpdateTimestamp`).

**`CitizenDocument`** → table `citizenDocument`
- `documentId` (PK, CHAR(36)), `citizenId` (CHAR(36) reference), `documentType`, `fileName`,
  `filePath`, `fileType`, `fileSizeKb`, `isDummy`, `issuedDate`, `expiryDate`, `status`,
  `verifiedBy`, `verifiedAt`, `uploadedAt`.
- `documentType` and `status` carry `@Check` constraints.
- `uploadedAt` auto-populated on insert.

**Why store `citizenId` as a String, not a `@ManyToOne`:** decoupling. The document table simply
holds the owning citizen's id; the module never imports another module's entity, so it builds and
ships on its own.

### 5.3 Repositories — `repository/`
Spring Data interfaces — method names become SQL automatically; `JpaRepository` adds CRUD
(`save`, `findById`, `findAll`, …).

| File | Methods | Backs |
|---|---|---|
| `CitizenProfileRepository` | `existsByEmail`, `existsByNationalIdNumber` | duplicate checks (409) at registration |
| | `findByWard` | GET `/getCitizensByWard/{ward}` |
| `CitizenDocumentRepository` | `findByCitizenId` | GET `/{citizenId}/getAllDocuments` |
| | `countByCitizenId` | "max 5 documents per citizen" rule |
| | `findByDocumentIdAndCitizenId` | GET document by id, scoped to its owner |

### 5.4 DTOs — `dto/request/` and `dto/response/`
Java `record`s defining the JSON contract. Request DTOs carry Bean Validation annotations
(`@NotBlank`, `@Email`) that the controller will enforce in Tier 2.

**Requests:** `RegisterCitizenRequest`, `UpdateCitizenProfileRequest`,
`UpdateCitizenStatusRequest`, `VerifyDocumentRequest`, `IssueDummyDocumentRequest`.

**Responses:** `CitizenProfileResponse` (national ID **masked**), `CitizenSummaryResponse`
(ward list item), `DocumentSummaryResponse` (list view — no `filePath`/`verifiedBy`),
`DocumentDetailResponse` (full detail incl. `filePath`).

**Why two document response shapes:** the list endpoint deliberately hides the file URL and
verifier; only the single-document detail endpoint exposes them. DTOs make that trivial.

---

## 6. Endpoint summary (v4) — target for Tier 2

| # | Method | Path (`/civicDesk/citizenProfile` + …) | Access | Returns |
|---|---|---|---|---|
| 1 | POST | `/registerCitizen` | Public | message |
| 2 | GET | `/getProfile/{citizenId}` | Authenticated | data |
| 3 | PUT | `/updateProfile/{citizenId}` | Citizen (own) | message |
| 4 | PUT | `/updateStatus/{citizenId}` | Admin / Dept. Supervisor | message |
| 5 | POST | `/{citizenId}/uploadDocument` | Citizen (own) | message |
| 6 | GET | `/{citizenId}/getAllDocuments` | Citizen / Staff / Admin | data |
| 7 | GET | `/{citizenId}/getDocumentById/{documentId}` | Citizen / Staff / Admin | data |
| 8 | PUT | `/{citizenId}/verifyDocument/{documentId}` | Field Officer / Compliance / Supervisor / Admin | message |
| 9 | POST | `/{citizenId}/uploadDocument` (`isDummy:true`) | Internal — Module 2.3 | message |
| 10 | GET | `/getCitizensByWard/{ward}` | Staff / Admin / Module 2.7 | data |

**Conventions:** GET → `{ "data": ... }`; POST/PUT → `{ "message": ... }`. PUT only (no PATCH).

---

## 7. Business rules to enforce in Tier 2

### Status transitions
- **Citizen:** `→ Active` on register; `Active → Verified`; `Active ↔ Flagged`; `Verified → Flagged`.
  (`Verified → Active` is not allowed.)
- **Document:** `→ Valid` on upload/issue; `Valid → Expired` (auto, past expiry); `Valid → Revoked`;
  `Expired → Revoked`.

### Validation & limits
- Required at register: `name`, `email`, `phone`, `ward`. Gender must be Male/Female/Other.
- Duplicate `email` or `nationalIdNumber` → 409.
- Documents: max **5 per citizen**; max **2 MB**; types **PDF/JPG/JPEG/PNG** (MIME-checked);
  stored on disk under a UUID name; `filePath` holds the full URL.
- National ID returned **masked** (e.g. `IND-****7890`).

---

## 8. How to build & verify (demo commands)

```powershell
# from the project root: c:\pruthvi\projects\INTDE26JFSA007-POD5-civicDesk

# 1) All changes are isolated to my module — no conflicts with teammates
git status --short

# 2) The code compiles
.\mvnw.cmd compile          # → BUILD SUCCESS

# 3) See the structure
tree /F src\main\java\com\civicdesk\module\citizen
```

**Tier 1 deliverable = "models the data correctly and compiles cleanly."** Running the app
against MySQL and calling endpoints is a Tier 2 demo (needs controllers + the temporary security
config + a running database).

---

## 9. Open coordination items (to raise with the team)

1. **`common/` ownership** — the `{ "data" }` / `{ "message" }` response envelope and the global
   exception handler are needed by every module. Agree who builds them before Tier 2.
2. **Table / column naming** — Grievance (2.5) and Service Requests (2.3) will foreign-key to
   `citizenProfile.citizenId`. Confirm the table name (`citizenProfile`) and `citizenId` column.
3. **JWT contract from IAM (2.1)** — needed for Tier 3: signing key/algorithm, claim names for
   user id and roles, exact role strings, and how a citizen's token maps to a `citizenId`.
4. **Module 2.3 (dummy document)** and **Module 2.7 (ward lookup)** — which role/identity they
   call with, so the access checks pass once auth is on.
5. **File base URL** — the spec example uses port `8080` but the app runs on `8081`; the stored
   `filePath` must match the real port (will be a config property in Tier 2).

---

## 10. Changelog

| Date | Change |
|---|---|
| 2026-06-11 | **Tier 1 complete.** Added 4 enums, 2 entities, 2 repositories, 9 DTOs (5 request, 4 response) under `module/citizen/`. Removed `.gitkeep` placeholders from the four populated folders. `./mvnw compile` → BUILD SUCCESS (17 classes). No shared files or other modules touched. |
| 2026-06-12 | **Tier 1 review & refine pass** (see [`Module-2.2-Tier1-Review-Log.md`](./Module-2.2-Tier1-Review-Log.md)). Verified naming-strategy/CHECK-constraint alignment; added 2 DB indexes (`ward`, `citizenId`), widened `fileType` 10→50, added `@Past` on `dateOfBirth`. Still BUILD SUCCESS. Open questions + blockers logged. |
