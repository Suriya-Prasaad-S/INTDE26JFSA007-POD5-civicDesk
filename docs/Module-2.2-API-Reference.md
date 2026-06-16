# CivicDesk — Module 2.2 (Citizen Profile & Registration) — API Reference

**Version:** v4 (verb-based paths) · **Owner:** Pruthiviraj · **Base path:** `/civicDesk/citizenProfile`
**Stack:** Java 21 · Spring Boot 3.4.1 · Spring Data JPA · MySQL · Bean Validation

Endpoint contract for Module 2.2. Companion artifacts: the Postman collection
(`postman/CivicDesk-Module2.2-Citizen.postman_collection.json`), the test matrix
(`docs/Module-2.2-API-Test-Plan.xlsx`), and the demo runbook (`docs/Module-2.2-API-Demo-Guide.xlsx`).

---

## 1. Conventions

| Aspect | Detail |
|---|---|
| **Base URL (local)** | `http://localhost:8081/civicDesk/citizenProfile` |
| **Auth** | None in this tier — JWT/role checks are Tier 3 (IAM). *Exception:* document verification is gated to a Department Supervisor (stand-in lookup, see #8). |
| **Tables** | snake_case: `citizen_profile`, `citizen_document` (columns also snake_case, e.g. `citizen_id`, `national_id_number`). |
| **IDs** | `citizenId` / `documentId` are **16-character alphanumeric** strings, generated server-side (was CHAR(36) UUID v4). |
| **Success — GET** | Returns the resource DTO or a JSON array, **200 OK**. |
| **Success — POST/PUT** | Returns `{ "message": "…" }` (uploads also include `documentId`). POST that creates → **201**; PUT → **200**. Registration returns the message **only** (no id). |
| **Status values** | Single-character codes. Citizen: `A`=Active, `V`=Verified, `F`=Flagged. Document: `V`=Valid, `E`=Expired, `R`=Revoked. |
| **National ID** | Returned **masked** (`****` + last 4 chars); never exposed in full. |
| **Error body** | `{ "message": "…" }` only (no timestamp/status/path). |
| **Files** | Uploaded bytes are stored on disk (`citizen.document.storage-dir`, default `./uploads`) and served by `GET /files/{name}`. |

### Error status codes
| Status | When |
|---|---|
| **400** | Validation failure; invalid enum/status code; update with no fields; empty/oversized/wrong-type upload; malformed JSON; missing multipart part/param |
| **403** | Verifier is not an Active Department Supervisor |
| **404** | Citizen, document, or file not found (document lookups are owner-scoped) |
| **409** | Duplicate email / national ID; illegal status transition; document limit (max 5) reached |
| **413** | Upload exceeds the servlet multipart cap |

---

## 2. Endpoint summary

| # | Method | Path (after base) | Access (target) | Returns |
|---|---|---|---|---|
| 1 | POST | `/registerCitizen` | Public | message |
| 2 | GET | `/getProfile/{citizenId}` | Authenticated | profile |
| 3 | PUT | `/updateProfile/{citizenId}` | Citizen (own) | message |
| 4 | PUT | `/updateStatus/{citizenId}` | Admin / Dept. Supervisor | message |
| 5 | POST | `/{citizenId}/uploadDocument` *(multipart)* | Citizen (own) | message + documentId |
| 6 | GET | `/{citizenId}/getAllDocuments` | Citizen / Staff / Admin | document list |
| 7 | GET | `/{citizenId}/getDocumentById/{documentId}` | Citizen / Staff / Admin | document detail |
| 8 | PUT | `/{citizenId}/verifyDocument/{documentId}` | **Department Supervisor** | message |
| 9 | GET | `/getCitizensByWard/{ward}` | Staff / Admin / Module 2.7 | citizen list |
| 10 | GET | `/getAllCitizens` | Staff / Admin | citizen list |
| 11 | GET | `/files/{filename}` | (serves a stored document) | file bytes |

---

## 3. Endpoints

### 1. Register Citizen — `POST /registerCitizen`
Creates a citizen (16-char id, status `A`). Rejects a duplicate email or national ID.

**Body:** `name`*, `ward`*, `email`* (valid, unique), `phone`* (10 digits); optional `dateOfBirth`
(past), `gender` (Male/Female/Other), `nationalIdNumber` (unique), `address`, `zone`.
```json
{ "name": "Meera Nair", "dateOfBirth": "1990-05-14", "gender": "Female",
  "nationalIdNumber": "IND1234567890", "address": "42 MG Road",
  "ward": "Ward-12", "zone": "North", "email": "meera.nair@example.com", "phone": "9876543210" }
```
**201:** `{ "message": "Citizen registered successfully" }` — *no id in the body*; obtain the new
`citizenId` from `getCitizensByWard` / `getAllCitizens`.
**Errors:** 400 · 409

### 2. Get Profile — `GET /getProfile/{citizenId}`
**200:**
```json
{ "citizenId": "9fK2pQ7mZ1xR4bN0", "name": "Meera Nair", "dateOfBirth": "1990-05-14",
  "gender": "Female", "nationalIdNumber": "****7890", "address": "42 MG Road",
  "ward": "Ward-12", "zone": "North", "email": "meera.nair@example.com", "phone": "9876543210",
  "status": "A" }
```
**Errors:** 404

### 3. Update Profile — `PUT /updateProfile/{citizenId}`
Patches mutable fields only (`name`/`address`/`ward`/`zone`/`phone`); at least one required.
`email`/`gender`/`dateOfBirth`/`nationalIdNumber` are immutable here.
```json
{ "name": "Meera N. Nair", "address": "55 Residency Road", "phone": "9999988888" }
```
**200:** `{ "message": "Citizen profile updated successfully" }` · **Errors:** 400 · 404

### 4. Update Status — `PUT /updateStatus/{citizenId}`
`status` is a single-char code. Allowed: `A→V`, `A↔F`, `V→F`. `V→A` is rejected.
```json
{ "status": "V" }
```
**200:** `{ "message": "Citizen status updated successfully" }` · **Errors:** 400 (bad code) · 404 · 409 (illegal transition)

### 5. Upload Document — `POST /{citizenId}/uploadDocument` *(multipart/form-data)*
Form fields: `file` (PDF/JPG/JPEG/PNG, ≤ 2 MB) and `documentType`. The bytes are stored on disk;
`filePath` is set to the retrieval URL. Rules: max 5 per citizen, ≤ 2 MB, allowed types (ext + MIME).
**201:** `{ "message": "Document uploaded successfully", "documentId": "Ab3Xy9Kp2Lm7Qr5" }`
**Errors:** 400 (empty/oversized/wrong-type/missing file or documentType) · 404 · 409 (limit) · 413

### 6. Get All Documents — `GET /{citizenId}/getAllDocuments`
Summary list (no `filePath` / `verifiedBy`). `status` is a single-char code.
```json
[ { "documentId": "Ab3Xy9Kp2Lm7Qr5", "documentType": "NationalID", "fileName": "id.pdf",
    "fileType": "pdf", "fileSizeKb": 128, "status": "V", "issuedDate": null, "expiryDate": null,
    "verifiedAt": null, "uploadedAt": "2026-06-16T09:50:00" } ]
```
**Errors:** 404. Empty list (`[]`) if none.

### 7. Get Document By Id — `GET /{citizenId}/getDocumentById/{documentId}`
Detail (includes `filePath`), owner-scoped.
```json
{ "documentId": "Ab3Xy9Kp2Lm7Qr5", "citizenId": "9fK2pQ7mZ1xR4bN0", "documentType": "NationalID",
  "fileName": "id.pdf", "filePath": "http://localhost:8081/civicDesk/citizenProfile/files/Ab3Xy9Kp2Lm7Qr5.pdf",
  "fileType": "pdf", "fileSizeKb": 128, "issuedDate": null, "expiryDate": null, "status": "V",
  "verifiedBy": null, "verifiedAt": null, "uploadedAt": "2026-06-16T09:50:00" }
```
**Errors:** 404

### 8. Verify Document — `PUT /{citizenId}/verifyDocument/{documentId}`
**Gated to a Department Supervisor:** `verifiedBy` must be an Active `DEPT_SUPERVISOR` in the `users`
table, else **403**. (Stand-in for the Tier-3 JWT role check.) Applies the manual status transition.
`status` single-char. Allowed: `V→V` (confirm), `V→R`, `E→R`. `E` is never manual; `R` is terminal.
```json
{ "verifiedBy": "20000000-0000-0000-0000-000000000002", "status": "V" }
```
**200:** `{ "message": "Document verified successfully" }` · **Errors:** 400 · 403 · 404 · 409

### 9. Get Citizens By Ward — `GET /getCitizensByWard/{ward}`
```json
[ { "citizenId": "9fK2pQ7mZ1xR4bN0", "name": "Meera Nair", "ward": "Ward-12", "status": "A" } ]
```
Empty list (`[]`), **not 404**, when the ward has none.

### 10. Get All Citizens — `GET /getAllCitizens`  *(optional listing)*
Same summary shape as #9, for every citizen.

### 11. Download File — `GET /files/{filename}`
Streams a stored document's bytes (the URL held in a document's `filePath`). Content type is derived
from the extension. **Errors:** 404 (file missing) · 400 (invalid name).

---

## 4. Status transition rules

**Citizen** (`A`/`V`/`F`): register → `A`; `A → V`; `A ↔ F`; `V → F`. (`V → A` not allowed.)
**Document** (`V`/`E`/`R`): upload → `V`; `V → V` (confirm); `V → R`; `E → R`;
`V → E` is automatic (past `expiryDate`, never manual); `R` is terminal.

## 5. Business rules
- Duplicate `email` / `nationalIdNumber` at registration → 409.
- National ID returned masked (`****` + last 4); full value never exposed.
- Documents: max 5 per citizen, max 2 MB, types PDF/JPG/JPEG/PNG (extension + MIME). Bytes stored on
  disk under a generated name; rolled back if the service rejects the upload.
- Document lookups are owner-scoped (a document resolves only under its owning `citizenId`).
- Verification requires an Active Department Supervisor (`verifiedBy` looked up in `users`); becomes a
  JWT role check once IAM integrates (Tier 3).
