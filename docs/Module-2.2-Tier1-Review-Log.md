# CivicDesk — Module 2.2: Tier 1 Review & Refinement Log

Companion to [`Module-2.2-Citizen-DevLog.md`](./Module-2.2-Citizen-DevLog.md). That file records how
Tier 1 was *built*; this file records the **review-and-refine pass** done on top of it, before
Tier 2 starts.

| | |
|---|---|
| **Module** | 2.2 — Citizen Profile & Registration |
| **Owner** | Pruthiviraj |
| **Branch** | `feature/citizenProfile` |
| **Scope of this session** | Review the existing Tier 1 data layer; apply safe, module-isolated refinements; record open questions. **No new layer added.** |
| **Date** | 2026-06-12 |
| **Build status after changes** | `.\mvnw.cmd compile` → **BUILD SUCCESS** (17 citizen classes) |

---

## 1. What we did today

Reviewed all **17 Tier 1 files** (4 enums, 2 entities, 2 repositories, 9 DTOs) field-by-field
against the spec and the business rules in the DevLog (§6–§7), then applied refinements that stay
entirely inside `module/citizen/` (no shared files touched). Everything still compiles.

### Changes applied

| # | File | Change | Why |
|---|---|---|---|
| 1 | `entity/CitizenProfile.java` | Added index `idx_citizenProfile_ward` on `ward` | `findByWard` (GET `/getCitizensByWard/{ward}`) would otherwise full-scan the table. `email` / `nationalIdNumber` are already indexed via their `unique=true`. |
| 2 | `entity/CitizenDocument.java` | Added index `idx_citizenDocument_citizenId` on `citizenId` | Three repo methods (`findByCitizenId`, `countByCitizenId`, `findByDocumentIdAndCitizenId`) all filter by `citizenId`; it is the module's hottest lookup column. |
| 3 | `entity/CitizenDocument.java` | `fileType` confirmed as **short extension**, column `length = 10` | Decision: store the extension (`pdf`/`jpg`/`jpeg`/`png`), lowercased by the service — not the full MIME type. (Briefly widened to 50 during review while the question was open; reverted once decided.) |
| 4 | `dto/request/RegisterCitizenRequest.java` | Added `@Past` on `dateOfBirth` | A date of birth can never be in the future. Field stays optional — `@Past` only fires when a value is present. |
| 5 | `dto/request/RegisterCitizenRequest.java` | `phone` now `@Pattern(\d{10})` | Decision: phone must be numeric and exactly 10 digits. |
| 6 | `dto/request/UpdateCitizenProfileRequest.java` | `phone` now `@Pattern(\d{10})` | Same rule on update; optional, so `@Pattern` only fires when a phone is supplied. |

### How we verified

```powershell
# from c:\pruthvi\projects\INTDE26JFSA007-POD5-civicDesk
.\mvnw.cmd compile     # → [INFO] BUILD SUCCESS
# 17 .class files under target\classes\...\module\citizen
```

The new indexes are additive; with `spring.jpa.hibernate.ddl-auto=update` Hibernate will `CREATE
INDEX` on next startup against MySQL (no data risk).

---

## 2. How we did it — review method

1. Read every file in `module/citizen/` (entity → repository → dto).
2. **Resolved the biggest unknown first:** the entities use explicit camelCase `@Table` names and
   raw-SQL `@Check` constraints (`gender in (...)`, `documentType in (...)`). Whether those align
   with the real DB columns depends on the Hibernate physical naming strategy. Checked
   `application.properties` → it sets `PhysicalNamingStrategyStandardImpl`, which keeps identifiers
   **verbatim** (no snake_case). So `documentType`/`gender`/`status` columns match the CHECK
   constraints, and the table names stay `citizenProfile` / `citizenDocument` as other modules
   expect. ✅ **Confirmed safe — no action needed.**
3. Walked each field against the spec's validation/limit rules (§7) and the endpoint contract (§6).
4. Applied only changes that are (a) clearly correct and (b) isolated to this module. Anything
   needing a product/team decision was parked, then confirmed with the owner (see §3).

---

## 3. Decisions confirmed (2026-06-12)

| Question | Decision | Applied as |
|---|---|---|
| `fileType` — extension vs full MIME? | **Short extension** (`pdf`/`jpg`/`jpeg`/`png`), lowercased by the service | `CitizenDocument.fileType` `length = 10`, comment updated |
| Phone format? | **Numeric, exactly 10 digits** | `@Pattern(regexp = "\\d{10}")` on `phone` in both register and update DTOs |

### Still-open review findings (not blocking)

| Finding | File | Notes |
|---|---|---|
| `IssueDummyDocumentRequest.isDummy` is not `@NotNull` | `dto/request` | Doc says it "must be true". Left to the service to validate (upload-vs-issue routing lives there). |
| `nationalIdNumber` request value isn't length-checked against the DB `length=50` | `RegisterCitizenRequest` | A `@Size(max=50)` would surface an over-length value as a clean 400 instead of an insert error. Low priority. |
| No `equals`/`hashCode` on entities | both entities | Standard for JPA entities used only via repositories; revisit only if they go into Sets/maps. |

---

## 4. What's next (Tier 2 — not started)

Per the DevLog's tiered plan, Tier 2 is **business logic + API**: service classes, REST controllers,
file storage, the shared `common/` response envelope + global exception handler, and a temporary
`permitAll` security config so the app runs locally.

Suggested first steps once the `common/` ownership (§5) is settled:
1. `CitizenService` + `CitizenController` for the read/register/update endpoints (1–4, 10).
2. Document upload/storage (extension stored in `fileType`, MIME-checked) + the document endpoints (5–9).
3. Temporary `permitAll` security config so the app boots against MySQL for a live demo.

---

## 5. Blockers & coordination items

> Things that stop Tier 2 from being purely "my module." **Most are team/lead decisions, not code.**

1. **🚧 Shared `common/` ownership (hard blocker for Tier 2).** The `{ "data" }` / `{ "message" }`
   response envelope and the global exception handler live in `com.civicdesk.common.*` (currently
   only `.gitkeep` placeholders) and are needed by *every* module. Shared files must not be changed
   unilaterally. **Recommendation: centrally *owned*, not freely *shared*** — one person builds it
   once, early; all six modules consume it read-only; changes go through that owner / a reviewed PR.
   (Rationale in the chat summary.) Need the team to name the owner before controllers can return a
   consistent contract.
2. **JWT contract from Module 2.1 (IAM)** — needed for *Tier 3*, not Tier 2: signing key/alg, claim
   names for user id + roles, exact role strings, citizen-token → `citizenId` mapping.
3. **Table/column name confirmation** — verified locally that `PhysicalNamingStrategyStandardImpl`
   keeps `citizenProfile` / `citizenId` verbatim, but Grievance (2.5) and Service Requests (2.3)
   plan to FK to `citizenProfile.citizenId` — worth a one-line confirm so nobody flips the naming
   strategy in the shared config.
4. **File base URL / port** — spec examples use `8080`; app runs on `8081`. Stored `filePath` must
   match the real port → should be a config property in Tier 2, not hard-coded.

### Blockers actually hit *this session*
- **Bash tool `/etc` permission noise on Windows.** Running `./mvnw.cmd` through the Bash tool spun
  up a Unix compat layer that spewed `/etc/hosts` permission errors and hid Maven's output.
  **Workaround:** run the Maven wrapper through **PowerShell** (`.\mvnw.cmd compile`).
- **Working-directory drift.** Running the wrapper shifted the shell's CWD mid-session, which briefly
  caused a doc to be written to a doubled path. **Workaround:** use absolute paths for file ops.

---

## 6. Information needed (status)

1. **`fileType`** — ✅ **Answered:** store the short extension.
2. **Phone format** — ✅ **Answered:** numeric, exactly 10 digits.
3. **`common/` envelope + exception handler ownership** — ⏳ recommendation made (owned, not shared);
   awaiting team to name the owner.
4. **Green light for Tier 2** — ⏳ pending item 3.

---

## 7. Changelog

| Date | Change |
|---|---|
| 2026-06-12 | **Tier 1 review pass.** Verified naming-strategy/CHECK-constraint alignment (safe). Added 2 DB indexes (`ward`, `citizenId`) and `@Past` on `dateOfBirth`. Confirmed two decisions: `fileType` = short extension (`length=10`); `phone` = numeric `@Pattern(\d{10})` on register + update. `.\mvnw compile` → BUILD SUCCESS (17 classes). No shared files or other modules touched. |
