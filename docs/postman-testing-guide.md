# Postman testing guide — Service Request module

A step-by-step walkthrough for testing every endpoint. Follow it top to bottom.

---

## PART A — Before you open Postman

1. **Start MySQL** (it must be running on `localhost:3306`, user `root` / password `root`).
2. **Start the app.** In a terminal at the project root:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
   Wait until you see `Started CivicDeskApplication in ... seconds`. Leave this terminal
   running. The app is now at **http://localhost:8082** and has auto-seeded dummy data.
3. **Dummy data you'll use** (created automatically on startup):
   - Departments: `dept-0004` (Citizen Services), `dept-0002` (Public Works)
   - Citizens: `citizen-0001` (Active — works), `citizen-0002` (Flagged — gives 403)
   - Services: `svc-0001`, `svc-0002` (Active), `svc-0003` (Inactive)

---

## PART B — Set up Postman

### Option 1 (recommended): import the ready-made collection
1. Open Postman.
2. Top-left → **Import**.
3. Choose the file `docs/civicdesk-serviceRequest.postman_collection.json`.
4. A collection **"CivicDesk - Service Request"** appears in the left sidebar with 3 folders.
5. Click the collection name → **Variables** tab. You'll see `baseUrl`, `serviceId`,
   `citizenId`, `departmentId`, `requestId`, `docId`. Leave them as-is for now.

Every request below is already built in the collection — you just open it and click **Send**.

### Option 2: build requests by hand
If you'd rather create each request yourself: click **New → HTTP Request**, then for each
endpoint set the **method**, paste the **URL**, and (for POST/PUT) go to the **Body** tab →
choose **raw → JSON** and paste the JSON shown below. Details per request are in Part C.

---

## PART C — Run the requests in this order

The base URL for every request is:
```
http://localhost:8082/civicDesk/serviceRequest
```

### --- CATALOG SERVICES ---

### 1. GET getAllServices
- **Method:** GET
- **URL:** `http://localhost:8082/civicDesk/serviceRequest/getAllServices`
- **Body:** none
- Click **Send**.
- **Expect 200** — a JSON array of the Active services (`svc-0001`, `svc-0002`).
- **Try the filter:** change URL to `.../getAllServices?category=Certificate` → only
  Certificate services. (`category` can be Certificate, Utility, Registration, Welfare.)

### 2. GET getService
- **Method:** GET
- **URL:** `.../getService/svc-0001`
- Click **Send**.
- **Expect 200** — full details of svc-0001, including `requiredDocuments`.
- **Try 404:** `.../getService/nope` → `404 Service not found...`.

### 3. POST createService
- **Method:** POST
- **URL:** `.../createService`
- **Headers:** `Content-Type: application/json` (Postman adds this automatically when Body =
  raw/JSON).
- **Body → raw → JSON:**
  ```json
  {
    "serviceName": "Marriage Certificate",
    "departmentId": "dept-0004",
    "category": "Certificate",
    "processingDays": 5,
    "requiredDocuments": ["NationalID", "ResidenceProof"],
    "fee": 120.00
  }
  ```
- Click **Send**. **Expect 201** — "Service created successfully...".
- **Try 409:** Send the exact same body again → `409` duplicate name.
- **Try 404:** change `departmentId` to `dept-9999` → `404 Department ... does not exist`.

### 4. PUT updateService
- **Method:** PUT
- **URL:** `.../updateService/svc-0003`
- **Body → raw → JSON:**
  ```json
  {
    "serviceName": "Drainage Connection",
    "processingDays": 12,
    "fee": 800.00,
    "status": "Active"
  }
  ```
- Click **Send**. **Expect 200** — "Service updated successfully." (svc-0003 is now Active.)
- **Try 400:** set `"processingDays": 0` and `"serviceName": ""` → `400` with field errors.
- **Try 404:** URL `.../updateService/nope` → `404`.

### --- SERVICE REQUESTS ---

### 5. POST submitRequest
- **Method:** POST
- **URL:** `.../submitRequest`
- **Body → raw → JSON:**
  ```json
  {
    "citizenId": "citizen-0001",
    "serviceId": "svc-0001"
  }
  ```
- Click **Send**. **Expect 201** — "Service request submitted successfully...".
  An officer is auto-assigned behind the scenes.
- **Try 403:** `"citizenId": "citizen-0002"` (Flagged) → `403`.
- **Try 422:** `"serviceId": "svc-0003"`... note: after step 4 svc-0003 is Active, so to see
  the Inactive 422 either skip step 4 or set svc-0003 back to Inactive first.
- **Try 404:** `"serviceId": "nope"` → `404`.
- > The response is only a confirmation message — it does NOT contain the requestId. You'll
  > get the requestId from the next step.

### 6. GET getAllRequests
- **Method:** GET
- **URL:** `.../getAllRequests`
- Click **Send**. **Expect 200** — an array of requests.
- **IMPORTANT — copy the requestId:** in the response, find `"requestId": "...."` and copy
  that UUID value.
  - **If using the imported collection:** click the collection → **Variables** tab → find
    the `requestId` row → paste into the **Current value** column. (No save needed.) Now
    `{{requestId}}` in later requests resolves to it.
  - **If building by hand:** just paste the UUID directly into the URLs of steps 7, 10, 11.
- **Try the filters:** `.../getAllRequests?status=Submitted` or
  `.../getAllRequests?departmentId=dept-0004` (both optional, can combine).

### 7. GET getRequest
- **Method:** GET
- **URL:** `.../getRequest/{{requestId}}`  (or paste the UUID instead of `{{requestId}}`)
- Click **Send**. **Expect 200** — full request details with a `documents` array (empty
  until you upload one in step 10).
- **Try 404:** `.../getRequest/nope` → `404`.

### 8. GET getRequestsByCitizen
- **Method:** GET
- **URL:** `.../getRequestsByCitizen/citizen-0001`
- Click **Send**. **Expect 200** — all requests that citizen submitted.

### 9. PUT updateRequestStatus
- **Method:** PUT
- **URL:** `.../updateRequestStatus/{{requestId}}`
- **Body → raw → JSON:**
  ```json
  {
    "newStatus": "UnderReview",
    "remarks": "Initial review started."
  }
  ```
- Click **Send**. **Expect 200** — "Status has been moved to UnderReview."
- **Walk the workflow** (Send each in turn, changing `newStatus`):
  `UnderReview` → `Approved` → `Completed`. All 200.
- **Try 422 (invalid jump):** on a brand-new Submitted request, send `"newStatus":"Approved"`
  → `422 Invalid status transition. ... Allowed next states: UnderReview.`
- **Try 422 (terminal):** after a request is `Completed`, send any status → `422 Request is
  in a terminal state.`
- Allowed transitions:
  `Submitted→UnderReview` · `UnderReview→PendingDocuments|Approved|Rejected` ·
  `PendingDocuments→UnderReview|Rejected` · `Approved→Completed|Rejected`.

### --- DOCUMENTS ---

> Documents can only be uploaded to a non-terminal request. If the request from step 5 is
> now Completed, submit a fresh one (repeat step 5) and use that new requestId here.

### 10. POST uploadDocument
- **Method:** POST
- **URL:** `.../uploadDocument/{{requestId}}`
- **Body tab → choose `form-data`** (NOT raw). Add two rows:

  | KEY | (hover the right edge of KEY to switch Text/File) | VALUE |
  |-----|------|-------|
  | `documentType` | keep as **Text** | `NationalID` |
  | `file` | switch the dropdown to **File** | click **Select Files** → pick any `.pdf`, `.jpg`, or `.png` |

- **Do NOT add a Content-Type header yourself** — Postman sets the multipart boundary
  automatically. (If you previously set `Content-Type: application/json`, delete it.)
- Click **Send**. **Expect 201** — "Document uploaded successfully...".
- **Try 400:** pick a `.txt` file instead → `400 Invalid file type...`.
- **Try 404:** use a bad requestId in the URL → `404`.

### 11. GET getDocuments
- **Method:** GET
- **URL:** `.../getDocuments/{{requestId}}`
- Click **Send**. **Expect 200** — array of the documents you uploaded, each with
  `verificationStatus: "Pending"`.
- **Copy a `docId`** from the response (same paste-into-variable trick as step 6, into the
  `docId` variable).
- **Try 404:** bad requestId → `404`.

### 12. PUT verifyDocument
- **Method:** PUT
- **URL:** `.../verifyDocument/{{docId}}`
- **Body → raw → JSON:**
  ```json
  {
    "verificationStatus": "Verified"
  }
  ```
- Click **Send**. **Expect 200** — "Document verified successfully...".
- **Try the rejection (side effect):** send `"verificationStatus": "Rejected"` on another
  docId → 200, and the **parent request automatically moves to `PendingDocuments`** (verify
  via getRequest in step 7).
- **Try 400:** send `"verificationStatus": "Pending"` → `400 ... must be Verified or Rejected`.
- **Try 404:** bad docId → `404`.

---

## Quick reference — all endpoints

| # | Method | Path | Body |
|---|--------|------|------|
| 1 | GET | `/getAllServices?category=` | — |
| 2 | GET | `/getService/{serviceId}` | — |
| 3 | POST | `/createService` | JSON |
| 4 | PUT | `/updateService/{serviceId}` | JSON |
| 5 | POST | `/submitRequest` | JSON |
| 6 | GET | `/getAllRequests?status=&departmentId=` | — |
| 7 | GET | `/getRequest/{requestId}` | — |
| 8 | GET | `/getRequestsByCitizen/{citizenId}` | — |
| 9 | PUT | `/updateRequestStatus/{requestId}` | JSON |
| 10 | POST | `/uploadDocument/{requestId}` | form-data |
| 11 | GET | `/getDocuments/{requestId}` | — |
| 12 | PUT | `/verifyDocument/{docId}` | JSON |

## Troubleshooting
- **Connection refused / can't reach** → the app isn't running, or not on port 8082. Check
  the terminal shows `Started CivicDeskApplication`.
- **401 Unauthorized** → you shouldn't get this; security is off. If you do, you added an
  auth header by accident — remove it.
- **500 on submitRequest** → check the app terminal log; usually means MySQL isn't reachable.
- **uploadDocument fails oddly** → make sure Body is `form-data`, the `file` row is type
  **File**, and there's no manual `Content-Type` header.
- **Reset all data** → stop the app, then in MySQL run
  `DROP DATABASE civicdesk; CREATE DATABASE civicdesk;` and restart — the seeder repopulates.
