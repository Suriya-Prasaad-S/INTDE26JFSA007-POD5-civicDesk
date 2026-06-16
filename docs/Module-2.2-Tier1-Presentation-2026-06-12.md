# Module 2.2 — Tier 1 Progress & Presentation Guide

**Date:** 2026-06-12 · **Owner:** Pruthiviraj · **Branch:** `feature/citizenProfile`
**For:** standup / progress demo. Companion to the detailed
[Tier 1 Review Log](./Module-2.2-Tier1-Review-Log.md) and [DevLog](./Module-2.2-Citizen-DevLog.md).

---

## A. What I did today (the summary)

Completed a **review-and-refine pass over the Tier 1 data layer** for the Citizen Profile module
and confirmed it builds cleanly. Tier 1 is now **done and demo-ready**.

- **Reviewed all 17 Tier 1 files** field-by-field (4 enums, 2 entities, 2 repositories, 9 DTOs).
- **Verified a hidden risk was safe:** the entities use camelCase table names + raw-SQL `@Check`
  constraints. Confirmed `application.properties` uses `PhysicalNamingStrategyStandardImpl`, so the
  DB columns stay verbatim (`citizenProfile.citizenId`, `documentType`, …) and match the constraints.
- **Applied 5 module-isolated refinements:**
  - DB index on `CitizenProfile.ward` (backs the ward lookup).
  - DB index on `CitizenDocument.citizenId` (backs all 3 document queries).
  - `@Past` on `dateOfBirth` (can't be a future date).
  - `phone` → `@Pattern(\d{10})` on register **and** update (numeric, exactly 10 digits).
  - `fileType` locked to the **short extension** (`pdf`/`jpg`/`jpeg`/`png`), column `length=10`.
- **Confirmed two product decisions** with the team (phone format; fileType = extension).
- **Result:** `.\mvnw.cmd compile` → **BUILD SUCCESS** (17 classes). Zero shared files touched,
  zero other modules touched.

---

## B. How to present it (≈5 minutes)

### 1. Open with the framing (one sentence)
> "Module 2.2 is built in tiers — data layer first. Tier 1 (the data foundation) is complete,
> reviewed, and compiling. It's fully self-contained in my module folder, so it has no dependency
> on auth or any teammate's code yet."

### 2. Live demo — two commands (run in **PowerShell**)
```powershell
cd C:\pruthvi\projects\INTDE26JFSA007-POD5-civicDesk

# (a) It models the data and compiles cleanly — the Tier 1 deliverable
.\mvnw.cmd compile          # → [INFO] BUILD SUCCESS

# (b) Show the structure — everything lives in one module folder
tree /F src\main\java\com\civicdesk\module\citizen
```
Then open **one entity and one DTO** to make the design concrete:
- `entity/CitizenProfile.java` — point at the enum + `@Check` constraint and the new `ward` index.
- `dto/response/CitizenProfileResponse.java` — point out `nationalIdNumber` is returned **masked**.

### 3. Key decisions to highlight (and the "why" — be ready to defend these)
| Decision | Why it's right |
|---|---|
| **Tiered, data-first build** | The data model is the hardest thing to change once other layers/modules depend on it, so it's settled first. Lets me make progress without waiting on the IAM module. |
| **No auth in this module** | Module 2.1 (IAM) owns JWT. Building it here would duplicate a teammate's work and couple the modules; the data layer doesn't need it. Auth lands in Tier 3. |
| **`citizenId` stored as a plain String in documents (not `@ManyToOne`)** | Keeps the module decoupled — it imports no other module's classes and compiles on its own. |
| **Enums + DB `@Check` constraints** | Invalid values (a typo like `"Mael"`) are impossible by construction, at both the Java and DB layers. |
| **Records as DTOs, separate from entities** | Lets me mask the national ID and expose different fields per endpoint, independent of the DB schema. |
| **Today's refinements** | Indexes = performance for the real query patterns; `@Past`/`@Pattern` = data integrity; `fileType` decision = unblocks Tier 2 upload code. |

### 4. Blockers / coordination to raise (shows you're thinking ahead)
- **Shared `common/` is owned by a teammate** — the `ApiResponse` envelope + `GlobalExceptionHandler`
  already exist there. For Tier 2 I'll **consume** them (need the contract: envelope shape +
  exception→status mapping), not rebuild them.
- **Teammate's latest push has bugs**, so I'm intentionally **not pulling/pushing** right now —
  Tier 1 is isolated to `module/citizen/`, so this doesn't block me.
- Confirm `citizenProfile` / `citizenId` naming for downstream FKs (Modules 2.3, 2.5).
- JWT contract from IAM is needed for Tier 3; file-serving base URL/port for Tier 2.

### 5. What's next
> "Tier 2 is business logic + REST controllers. It's ready to start as soon as I get the
> `common/` envelope + exception contract from the teammate — and once their buggy push is fixed
> so I can safely integrate."

---

## C. Likely questions (prep)

- **"Did you run it against the database / call the endpoints?"** → That's Tier 2 (needs
  controllers + a temporary security config + a running MySQL). The Tier 1 deliverable is
  *"models the data correctly and compiles cleanly,"* which is what I'm showing.
- **"Why isn't there any authentication?"** → IAM (2.1) owns it; it's mocked/deferred here by
  design so I don't duplicate or couple to a teammate's module. Added in Tier 3.
- **"What about your teammate's bugs?"** → They're in shared code I don't depend on yet; my Tier 1
  is fully isolated, so I'm unaffected and staying on my own branch until the fix lands.
