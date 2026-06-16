# CivicDesk — Module 2.2 (Citizen Profile) — INSTRUCTIONS / RUNBOOK

> One place for **everything operational**: set up → run → test in Postman → demo → reset →
> troubleshoot. For the API contract see [`Module-2.2-API-Reference.md`](./Module-2.2-API-Reference.md);
> for project context see [`Module-2.2-CONTEXT.md`](./Module-2.2-CONTEXT.md).
> All commands are **PowerShell**, run from the project folder
> `c:\pruthvi\projects\INTDE26JFSA007-POD5-civicDesk`.

---

## 0. Prerequisites (one-time)

- **MySQL** running on `localhost:3306`, user `root`, password `root`. Check:
  ```powershell
  Get-Service '*mysql*'      # Status should be Running
  ```
- **Postman** and (optionally) **MySQL Workbench** installed.
- The `mysql` CLI is at `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe` (not on PATH — use the
  full path, or open it in Workbench).

---

## 1. Reset the database (clean slate)

Tables are **snake_case** (`citizen_profile`, `citizen_document`). If you previously ran an older
build, drop & recreate so only the new tables exist:
```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -proot -e "DROP DATABASE IF EXISTS civicdesk; CREATE DATABASE civicdesk CHARACTER SET utf8mb4;"
```

## 2. Run the application

```powershell
cd c:\pruthvi\projects\INTDE26JFSA007-POD5-civicDesk
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local" "-Dmaven.resolver.transport=wagon" "-Dmaven.wagon.http.ssl.insecure=true" "-Dmaven.wagon.http.ssl.allowall=true" "-Dmaven.wagon.http.ssl.ignore.validity.dates=true"
```
- The **`local` profile** seeds mock `users`/`departments` and raises upload limits.
- The **wagon SSL flags** are required on the corporate network (Maven Central is proxy-blocked).
- **Wait for** `Started CivicDeskApplication in NN seconds` before doing anything — tables are created
  during startup. Leave this terminal running; **Ctrl+C** stops the app (answer `Y` to
  "Terminate batch job").

## 3. Confirm it started & view the tables

In Workbench or via the CLI (note: query the **`civicdesk`** schema):
```sql
SHOW TABLES FROM civicdesk;                       -- citizen_profile, citizen_document, users, departments
SELECT COUNT(*) FROM civicdesk.citizen_profile;   -- 0 (empty until you register)
SELECT COUNT(*) FROM civicdesk.users;             -- 7 (mock data)
SELECT COUNT(*) FROM civicdesk.departments;       -- 3 (mock data)
```
An **empty** `citizen_profile` is correct before testing — it is not the same as "table doesn't exist".

## 4. Test in Postman

1. **Import** `postman/CivicDesk-Module2.2-Citizen.postman_collection.json` (Import → drop the file).
   `baseUrl` is preset to `http://localhost:8081/civicDesk/citizenProfile`.
2. **Run requests top-to-bottom.** They auto-chain: *Register* captures `citizenId` (via a ward
   lookup, since register returns no id); *Upload* captures `documentId`; *Get Document By Id*
   captures the stored file name.

| Order | Request | Action / body | Expect |
|---|---|---|---|
| 1 | Register Citizen | Send (body pre-filled, unique each run) | **201** `{message}` |
| 2 | Get Profile | Send | **200**, `nationalIdNumber` = `****…`, `status` = `A` |
| 3 | Update Profile | Send | **200** |
| 4 | Update Status | Send (`{"status":"V"}`) | **200** |
| 5 | Get Citizens By Ward | Send | **200**, array |
| 6 | Get All Citizens *(optional)* | Send | **200**, array |
| 7 | Upload Document | **attach a PDF/JPG/PNG ≤2 MB** in form-data `file` → Send | **201** `{message, documentId}` |
| 8 | Get All Documents | Send | **200**, array |
| 9 | Get Document By Id | Send | **200**, includes `filePath` |
| 10 | Download File | Send | **200**, the file bytes |
| 11 | Verify Document | Send (`verifiedBy` = supervisor, `{"status":"V"}`) | **200** |

> **Status is a single character:** citizen `A`/`V`/`F`, document `V`/`E`/`R`.
> **Upload now needs a real file** (the no-file "dummy" path was removed). Attach one in request #7.
> **Verify is gated to a Department Supervisor** — `verifiedBy` must be
> `20000000-0000-0000-0000-000000000002` (Asha Menon) or `70000000-…-007` (Sanjay Gupta).

After each write, refresh in Workbench:
```sql
SELECT * FROM civicdesk.citizen_profile;
SELECT * FROM civicdesk.citizen_document;
```

## 5. Error / negative tests (prove the rules)

Edit a request and re-Send:

| Try | Expect |
|---|---|
| Register the same email twice | **409** |
| Register with `"phone":"123"` | **400** |
| `GET /getProfile/does-not-exist` | **404** |
| Update status `V` then send `{"status":"A"}` | **409** (V→A blocked) |
| `updateStatus` with `{"status":"X"}` | **400** (allowed codes A, V, F) |
| Upload a `.txt` or a file > 2 MB | **400** |
| Upload a 6th document for one citizen | **409** |
| Verify with `verifiedBy` = a FIELD_OFFICER (`40000000-…-004`) | **403** |
| `getDocumentById` with another citizen's id | **404** |

All errors return `{ "message": "…" }` only.

## 6. Demo narration (quick script)

1. Show empty `citizen_profile`. → 2. **Register** (201) → row appears, `status=A`.
3. **Get Profile** → national ID masked (`****`), `status` is `A` not "Active".
4. **Update Status** `A→V` (200), then `V→A` → **409** (illegal transition).
5. **Upload** a real file (201) → bytes land in `./uploads`; **Download File** returns them.
6. **Verify** as the supervisor (200) → `verified_by`/`verified_at` set; verify as a non-supervisor → **403**.
7. **Get Citizens By Ward** / **Get All Citizens** → staff listings.
Close with **Swagger** (§8) to show the auto-generated docs.

## 7. Reset anytime (start fresh)

1. Stop the app (Ctrl+C, answer `Y`).
2. Drop & recreate the DB (§1).
3. Restart the app (§2) — it rebuilds the tables and reseeds mock data.
4. Re-run Postman from *Register*.

## 8. Swagger UI

With the app running: **http://localhost:8081/swagger-ui.html** — interactive docs for all endpoints
(no auth, since the security starter is disabled). Raw spec: `http://localhost:8081/v3/api-docs`.

## 9. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `'.\mvnw.cmd' is not recognized` | You're not in the project folder — `cd` into `…\INTDE26JFSA007-POD5-civicDesk` first |
| `PKIX path building failed` / plugin "could not be resolved" | Proxy SSL — use the **wagon flags** (§2) |
| `Table 'civicdesk.citizen_profile' doesn't exist` | App not fully started yet (wait for `Started CivicDeskApplication`), or you reset the DB and haven't restarted |
| `Table 'civicdesk_m22.…' doesn't exist` | You're querying the **wrong schema** — use `civicdesk` (set it as the default schema in Workbench) |
| Stuck at `Attaching agents: []` | Normal cold start (~11–40s). If it never finishes, a previous instance is holding port 8081 — stop it (Ctrl+C → `Y`, or kill stray `java`) |
| 401 on every endpoint | Security wasn't disabled — confirm the `spring-boot-starter-security` block is commented out in `pom.xml`, then rebuild |
| `mysql` not recognized | Use the full path `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`, or use Workbench |
| Verify returns 403 | `verifiedBy` is not a Department Supervisor — use `20000000-…-002` |

## 10. Quick reference

- **Base URL:** `http://localhost:8081/civicDesk/citizenProfile`
- **Run profile:** `local` · **DB:** `civicdesk` (root/root) · **Port:** 8081
- **IDs:** 16-char alphanumeric. **Status:** citizen `A/V/F`, document `V/E/R`.
- **Supervisor userId (verify):** `20000000-0000-0000-0000-000000000002`
- **Upload dir:** `./uploads` (configurable: `citizen.document.storage-dir`)
- **Endpoints:** registerCitizen, getProfile, updateProfile, updateStatus, getCitizensByWard,
  getAllCitizens, uploadDocument, getAllDocuments, getDocumentById, verifyDocument, files/{name}
- **Error codes:** 400 (validation/bad code/bad file), 403 (not supervisor), 404 (not found),
  409 (duplicate / illegal transition / doc limit), 413 (upload too large)
