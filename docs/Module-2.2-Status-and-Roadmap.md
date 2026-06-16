# CivicDesk — Module 2.2: Status & Roadmap

> ⚠️ **Historical (pre-2026-06-16 refactor).** Intent and cross-module needs still hold, but several
> specifics have since changed: IDs are now **16-char alphanumeric** (not UUID), tables/columns are
> **snake_case**, status is **single-char** (`A/V/F`, `V/E/R`), the **issue-dummy endpoint was
> removed**, register returns **no id**, real file storage was added, and verify is **gated to a
> Department Supervisor**. For the current truth read [`Module-2.2-CONTEXT.md`](./Module-2.2-CONTEXT.md)
> (§6) and [`Module-2.2-API-Reference.md`](./Module-2.2-API-Reference.md).

**The single "where we are / where we're going / what we need" reference for the Citizen module.**
Companion to the [DevLog](./Module-2.2-Citizen-DevLog.md) (how Tier 1 was built),
the [Tier 1 Review Log](./Module-2.2-Tier1-Review-Log.md) (the review-and-refine pass), and the
[Tier 1 Presentation Guide](./Module-2.2-Tier1-Presentation-2026-06-12.md) (the standup demo script).

| | |
|---|---|
| **Module** | 2.2 — Citizen Profile & Registration (+ Citizen Documents) |
| **Owner** | Pruthiviraj |
| **Branch** | `feature/citizenProfile` |
| **Base path** | `/civicDesk/citizenProfile` |
| **API spec** | API Reference **v4** (verb-based paths) — supersedes v3 |
| **Stack** | Java 21 · Spring Boot 3.4.1 · Spring Data JPA · MySQL · Bean Validation |
| **Auth** | JWT from Module 2.1 (IAM) — owned by a teammate, **not** implemented here |
| **Last updated** | 2026-06-14 |
| **Build status** | `.\mvnw.cmd clean compile` → **BUILD SUCCESS** (18 source files) |

---

## 0. Executive summary

The module is built in **three tiers** (data → logic → auth) so work can proceed without waiting on
the IAM module. **Tier 1 (the data foundation) is complete, reviewed, refactored, and compiling.**
Tier 2 (business logic + REST API) is **not started** and is **partially blocked** — not by code, but
by one team decision (who owns the shared `common/` response envelope) and by needing a few contracts
from other modules. Tier 3 (real auth) waits on Module 2.1 landing.

| Tier | Scope | Status |
|---|---|---|
| **Tier 1** | Entities, enums, repositories, DTOs | ✅ **Done** (built → reviewed → refactored) |
| **Tier 2** | Services, controllers, file storage, shared envelope, temp security | ⏳ **Not started** — service layer unblocked; controllers blocked on `common/` ownership |
| **Tier 3** | JWT security, ownership checks, role-based 403s | 🔒 **Blocked** — waits on Module 2.1 (IAM) |

---

## 1. WHAT IS DONE — Tier 1 (data foundation)

All under `com.civicdesk.module.citizen`. **17 citizen classes**, zero shared files touched, zero
other modules touched. Compiles cleanly on its own.

### 1.1 Enums — `entity/enums/`
Lock allowed values at the Java layer; mirrored by DB `@Check` constraints on the entities, so an
invalid value (a typo like `"Mael"`) is impossible by construction at **both** layers.

| Enum | Values |
|---|---|
| `Gender` | Male, Female, Other |
| `CitizenStatus` | Active, Verified, Flagged |
| `DocumentStatus` | Valid, Expired, Revoked |
| `DocumentType` | NationalID, ResidenceProof, BirthCertificate, IncomeCertificate |

> **2026-06-14 refactor:** the four enums were moved out of `entity/` into a dedicated
> `entity/enums/` sub-package (`com.civicdesk.module.citizen.entity.enums`) to keep `entity/` to just
> the two `@Entity` classes. Imports were added to `CitizenProfile`/`CitizenDocument`; nothing outside
> the module referenced them, so the change was self-contained. Verified with `clean compile`
> (incremental `compile` falsely reported "nothing to compile" — see DevLog note on `clean`).

### 1.2 Entities — `entity/`
JPA classes that become MySQL tables on startup (`spring.jpa.hibernate.ddl-auto=update`).

**`CitizenProfile`** → table `citizenProfile`
- Fields: `citizenId` (PK, CHAR(36)), `name`, `dateOfBirth`, `gender`, `nationalIdNumber` (unique),
  `address`, `ward`, `zone`, `email` (unique), `phone`, `status`, `createdAt`, `updatedAt`.
- `gender`/`status` carry `@Check` constraints so the DB rejects bad values too.
- `createdAt`/`updatedAt` auto-populated (`@CreationTimestamp` / `@UpdateTimestamp`).
- Index `idx_citizenProfile_ward` on `ward` (backs the ward lookup).

**`CitizenDocument`** → table `citizenDocument`
- Fields: `documentId` (PK, CHAR(36)), `citizenId` (CHAR(36) reference), `documentType`, `fileName`,
  `filePath`, `fileType` (short extension, `length=10`), `fileSizeKb`, `isDummy`, `issuedDate`,
  `expiryDate`, `status`, `verifiedBy`, `verifiedAt`, `uploadedAt`.
- `documentType`/`status` carry `@Check` constraints; `uploadedAt` auto-populated on insert.
- Index `idx_citizenDocument_citizenId` on `citizenId` (backs all three document queries).

> **Decoupling decision:** `citizenId` is stored as a plain `String`, **not** a `@ManyToOne`. The
> module imports no other module's entity, so it builds and ships independently.

### 1.3 Repositories — `repository/`
Spring Data interfaces — method names become SQL automatically; `JpaRepository` adds CRUD.

| Repository | Methods | Backs |
|---|---|---|
| `CitizenProfileRepository` | `existsByEmail`, `existsByNationalIdNumber` | duplicate checks (409) at registration |
| | `findByWard` | GET `/getCitizensByWard/{ward}` |
| `CitizenDocumentRepository` | `findByCitizenId` | GET `/{citizenId}/getAllDocuments` |
| | `countByCitizenId` | "max 5 documents per citizen" rule |
| | `findByDocumentIdAndCitizenId` | GET document by id, scoped to its owner |

### 1.4 DTOs — `dto/request/` and `dto/response/`
Java `record`s defining the JSON contract, separate from entities so we can mask sensitive fields and
expose different shapes per endpoint.

- **Requests (5):** `RegisterCitizenRequest`, `UpdateCitizenProfileRequest`,
  `UpdateCitizenStatusRequest`, `VerifyDocumentRequest`, `IssueDummyDocumentRequest`.
  Validation already on them: `@NotBlank`/`@Email`, `@Past` on `dateOfBirth`, `@Pattern(\d{10})` on
  `phone` (register + update).
- **Responses (4):** `CitizenProfileResponse` (national ID **masked**), `CitizenSummaryResponse`
  (ward list item), `DocumentSummaryResponse` (list view — no `filePath`/`verifiedBy`),
  `DocumentDetailResponse` (full detail incl. `filePath`).

### 1.5 Review & refinements applied (2026-06-12)
2 DB indexes (`ward`, `citizenId`), `@Past` on `dateOfBirth`, `@Pattern(\d{10})` on `phone`,
`fileType` locked to the short extension (`length=10`). Verified the camelCase `@Table`/`@Check`
names align with `PhysicalNamingStrategyStandardImpl` (identifiers kept verbatim) — **safe**.

### 1.6 Carried-over Tier 1 findings (non-blocking, decide during Tier 2)
- `IssueDummyDocumentRequest.isDummy` is not `@NotNull` — left for the service to validate.
- `nationalIdNumber` request value isn't `@Size(max=50)`-checked against the DB column.
- No `equals`/`hashCode` on entities — fine while used only via repositories.

---

## 2. WHAT NEEDS TO BE DONE — and HOW

### Tier 2 — Business logic + REST API  *(next)*

The layering is **Controller → Service → Repository → Entity**, with DTOs at the edges. Tier 1 built
the bottom two layers; Tier 2 builds the top two plus file storage and the cross-cutting response
contract.

#### 2.1 Service layer — `service/` *(✅ unblocked, start here)*
Pure business logic inside the module folder — no shared files, no auth needed. **This is the
recommended first step** because it depends only on Tier 1, which is done.

- **`CitizenService`** — wires in `CitizenProfileRepository`:
  - `registerCitizen`: generate `citizenId` (UUID v4); reject duplicate `email` /
    `nationalIdNumber` (`existsBy…` → **409**); set status `Active`; persist.
  - `getProfile`: load by id (**404** if missing); return `CitizenProfileResponse` with the national
    ID **masked** (e.g. `IND-****7890`).
  - `updateProfile`: patch allowed fields only.
  - `updateStatus`: enforce the transition rules below; reject illegal jumps (**409/400**).
  - `getCitizensByWard`: map `findByWard` → `List<CitizenSummaryResponse>`.
- **`DocumentService`** (or fold into `CitizenService`) — wires in `CitizenDocumentRepository`:
  - `uploadDocument`: enforce **max 5 per citizen** (`countByCitizenId`), **max 2 MB**, type in
    **PDF/JPG/JPEG/PNG** (MIME-checked, extension lowercased into `fileType`); store the file on
    disk under a **UUID file name** (prevents path-traversal); set `filePath` to the full retrieval
    URL; status `Valid`.
  - `issueDummyDocument` (`isDummy:true`, internal — Module 2.3): system-issued path; validate
    `isDummy` must be true here.
  - `getAllDocuments` → `List<DocumentSummaryResponse>`; `getDocumentById` (scoped to owner via
    `findByDocumentIdAndCitizenId`) → `DocumentDetailResponse`.
  - `verifyDocument`: set `status`, `verifiedBy`, `verifiedAt`; enforce document transitions.

**Status transitions to enforce (the heart of the service layer):**
- **Citizen:** `→ Active` on register; `Active → Verified`; `Active ↔ Flagged`; `Verified → Flagged`.
  `Verified → Active` is **not** allowed.
- **Document:** `→ Valid` on upload/issue; `Valid → Expired` (auto, past `expiryDate`);
  `Valid → Revoked`; `Expired → Revoked`.

#### 2.2 REST controllers — `controller/` *(🚧 blocked on §3.1 `common/` ownership)*
One `CitizenController` exposing the 10 v4 endpoints, base path `/civicDesk/citizenProfile`.
- `@Valid` on request bodies (the DTO annotations from Tier 1 then fire automatically).
- Return the **shared response envelope** (see §3.1): GET → `{ "data": … }`, POST/PUT →
  `{ "message": … }`. PUT only — no PATCH.
- Errors surface through the **shared global exception handler** (duplicate → 409, not-found → 404,
  validation → 400), so controllers stay thin.

| # | Method | Path (after base) | Access | Returns |
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

> Access column = the **target** state once Tier 3 auth is on. In Tier 2 every endpoint is reachable
> via a temporary `permitAll` config (§2.4) so the app can be demoed.

#### 2.3 File storage
Local disk store: write uploads under a UUID name, keep originals' extension only in `fileType`,
build `filePath` as the full retrieval URL using a **configurable base URL/port** (see §3.5 — must
not be hard-coded to `8080`; the app runs on `8081`).

#### 2.4 Temporary security config *(🚧 touches shared `config/` — needs team agreement)*
A `permitAll` Spring Security config so the app boots and endpoints are callable for a live demo
**before** real auth exists. Lives in shared `config/` → must be agreed with the team, not dropped in
unilaterally.

### Tier 3 — Auth integration  *(🔒 after Module 2.1 lands)*
Real JWT security filter; "own profile only" ownership checks (endpoints 3, 5); role-based 403s per
the access column; switch `verifiedBy` from the request body to the JWT subject. Requires the JWT
contract from IAM (§3.2).

---

## 3. WHAT IT NEEDS FROM OTHER MODULES / THE TEAM

> Most of these are **decisions or contracts, not code**. They are what stops Tier 2/3 from being
> purely "my module." Listed by who owns them.

### 3.1 🚧 Shared `common/` — response envelope + global exception handler  *(hard blocker for controllers)*
- **What:** the `{ "data" }` / `{ "message" }` response wrapper and the `GlobalExceptionHandler`
  (exception → HTTP status mapping). They live in `com.civicdesk.common.*`, **currently empty
  `.gitkeep` placeholders** — not yet built.
- **Why it blocks us:** every module must return the *same* contract, so this can't be built six
  times. Controllers (§2.2) can't return a consistent response until it exists.
- **Need from team:** **name one owner** to build it once, early; all modules consume it read-only;
  changes go through that owner / a reviewed PR. (Recommendation: *owned, not freely shared.*)
- **Exact contract we need:** the envelope class name + shape, and the exception→status mapping
  (so our service throws the right exception types: duplicate→409, not-found→404, validation→400).

### 3.2 🔒 Module 2.1 (IAM) — JWT contract  *(needed for Tier 3)*
**Confirmed** from the fetched IAM README (`origin/feature/iam`, commit `95f88c3`, 2026-06-13):
- **Signing/claims:** JWT HS256; claims are `userId`, `role`, `email`. Read via
  `SecurityContextUtil.getCurrentUserId()` / `getCurrentRole()` — never parsed by us.
- **Role codes (canonical — "do not rename"):** `CIT`, `FO`, `DS`, `ENG`, `CO`, `ADM`.
- **Response envelope:** `{ success, statusCode, message, data, timestamp }` via
  `ApiResponse.data(...)` (GET) / `ApiResponse.of(msg, null)` (POST/PUT).
- **Shared contract files:** `Role.java`, `JwtUtil.java`.

Two questions remain open and **block** the ownership/role checks — see §3.2a.

### 3.2a 🚩 Open questions for IAM — with rationale  *(blocking; raised with Suriya 2026-06-15)*

**Q1 — How does the JWT `userId` map to my `citizenId`?**
- *What it blocks:* the "Citizen (own)" guard on `PUT /citizenProfile/updateProfile/{citizenId}` and
  `POST /citizenProfile/{citizenId}/uploadDocument`. `getCurrentUserId()` returns the IAM id
  (e.g. `"100023"`) but my records are keyed by a CHAR(36) UUID `citizenId`. Different id spaces → I
  can't tell whether the caller owns the `{citizenId}` in the path, so the ownership check can't be
  written at all — any citizen could edit / upload to anyone's record.
- *Why it's urgent (data-model, not just auth):* citizens self-register via `/iam/auth/register`
  first, so I need to know whether `citizenId` should **equal** the IAM `userId` (1:1 — I stop
  generating my own UUID) or whether there's a separate `userId → citizenId` lookup. This decides my
  **primary-key strategy** — the hardest thing to change once Grievance (2.5) and Service Requests
  (2.3) FK to `citizenProfile.citizenId`.

**Q2 — What does "Compliance" mean on `verifyDocument` (#8) — a role or a department?**
- *What it blocks:* the single `@PreAuthorize("hasAnyRole(...)")` on
  `PUT /citizenProfile/{citizenId}/verifyDocument/{documentId}`. My spec lists its access as
  *"Field Officer / Compliance / Supervisor / Admin."* Mapping to the canonical codes
  (`CIT/FO/DS/ENG/CO/ADM`):
  - Field Officer → `FO` ✅
  - Admin → `ADM` ✅
  - Supervisor → `DS` *(confirm?)*
  - **"Compliance" → no matching role.** `CO` is *Coordinator*, and "Compliance & Audit" is a
    **department**, not a role.
- *Why the distinction matters:* if "Compliance" is the `CO` **role**, it's a one-line `hasAnyRole`.
  If it means the "Compliance & Audit" **department**, role-based `@PreAuthorize` isn't enough — I'd
  have to load the caller's department and check membership, a different and more complex guard.
  Guess wrong → wrong staff can verify documents, or the right staff get 403'd.

### 3.3 Shared `config/` — naming-strategy stability  *(confirm, don't change)*
We verified locally that `PhysicalNamingStrategyStandardImpl` keeps identifiers verbatim, so our
`citizenProfile` table and `citizenId` column names hold. **Need a one-line confirm that nobody flips
the naming strategy in the shared config**, since other modules FK to these names (§3.4).

### 3.4 Module 2.5 (Grievance) & Module 2.3 (Service Requests) — FK target confirmation
Both plan to foreign-key to `citizenProfile.citizenId`. **Confirm they're keying to the table name
`citizenProfile` and column `citizenId`** (CHAR(36) UUID v4) so nothing breaks when they integrate.

### 3.5 Module 2.3 (dummy document) & Module 2.7 (ward lookup) — calling identity
- **2.3** calls endpoint 9 (`uploadDocument` with `isDummy:true`) — need the role/identity it calls
  with so the access check passes once auth is on.
- **2.7** calls endpoint 10 (`getCitizensByWard`) — same question.

### 3.6 File base URL / port  *(config decision)*
Spec examples use port `8080`; the app runs on `8081`. The stored `filePath` must match the real
port → make it a **config property** in Tier 2, not a hard-coded value.

---

## 4. Suggested execution order

1. **Now (unblocked):** build `CitizenService` + `DocumentService` — all the business rules in §2.1.
   100% inside `module/citizen/`, no shared files, no auth.
2. **In parallel (people, not code):** get the team to name the `common/` owner (§3.1) and confirm
   the FK naming (§3.3–3.4).
3. **When `common/` lands:** build `CitizenController` (§2.2) + file storage (§2.3) against the real
   envelope; agree + add the temporary `permitAll` config (§2.4); demo against MySQL.
4. **When Module 2.1 lands:** Tier 3 — real JWT, ownership checks, role 403s, `verifiedBy` from token.

---

## 5. Risks & guardrails
- **Don't touch shared files unilaterally** — `pom.xml`, `config/`, `common/` are team-owned. The
  `common/` envelope and the temp security config both fall here.
- **Stay on `feature/citizenProfile`, don't pull/push yet** — a teammate's recent push has bugs;
  Tier 1 + the service layer are isolated to `module/citizen/`, so this doesn't block local progress.
- **`clean` before trusting a build** after any file move/rename/delete — incremental `compile` can
  falsely skip changed files (seen during the enum refactor).

---

## 6. Changelog
| Date | Change |
|---|---|
| 2026-06-11 | **Tier 1 complete.** 4 enums, 2 entities, 2 repositories, 9 DTOs under `module/citizen/`. `mvnw compile` → BUILD SUCCESS (17 classes). |
| 2026-06-12 | **Tier 1 review & refine pass.** Naming/CHECK alignment verified; 2 indexes, `@Past`, `@Pattern(\d{10})`, `fileType` extension. BUILD SUCCESS. |
| 2026-06-14 | **Enum refactor + this roadmap.** Moved 4 enums into `entity/enums/`; added imports to the two entities. `clean compile` → BUILD SUCCESS (18 source files). Authored this Status & Roadmap doc consolidating done / to-do / cross-module needs. |
| 2026-06-15 | **IAM contract confirmed (fetch, read-only).** Read `origin/feature/iam` README templates: pinned the `ApiResponse` envelope, role codes (`CIT/FO/DS/ENG/CO/ADM`), JWT claims (`userId/role/email`), and `SecurityContextUtil`/audit templates into §3.2. Logged the two blocking open questions with rationale (§3.2a) — raised with Suriya. Not yet on `main`, so still no compile dependency taken. |
