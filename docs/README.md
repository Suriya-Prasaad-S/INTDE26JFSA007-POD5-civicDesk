# Module 2.2 (Citizen Profile) — Documentation Index

**New here (developer or AI model)? Start with [`Module-2.2-CONTEXT.md`](./Module-2.2-CONTEXT.md).**
It is the self-contained handoff brief. Then use the runbook and API reference below.

> Everything reflects the **2026-06-16 refactor** unless marked *historical*. Current contract in one
> line: 16-char alphanumeric IDs · snake_case tables (`citizen_profile`, `citizen_document`) ·
> single-char status (citizen `A/V/F`, document `V/E/R`) · `{message}`-only responses & errors ·
> real file storage · `verifyDocument` gated to a Department Supervisor · no JUnit (Postman + docs).

## Read in this order (current / authoritative)

| # | File | What it gives you |
|---|---|---|
| 1 | [`Module-2.2-CONTEXT.md`](./Module-2.2-CONTEXT.md) | **Start here.** What the module is, stack, architecture, key decisions & deviations, file map, env quirks, status. |
| 2 | [`Module-2.2-INSTRUCTIONS.md`](./Module-2.2-INSTRUCTIONS.md) | Step-by-step runbook: set up → run → test in Postman → demo → reset → troubleshoot. |
| 3 | [`Module-2.2-API-Reference.md`](./Module-2.2-API-Reference.md) | Full endpoint contract (request/response, status codes, transitions). Also as `Module-2.2-API-Reference.xlsx`. |
| 4 | `Module-2.2-API-Test-Plan.xlsx` | Happy-path + negative test matrix (what / how / why). |
| 5 | `Module-2.2-API-Demo-Guide.xlsx` | Setup · Demo Script · Reset runbook (spreadsheet form). |
| — | `../postman/CivicDesk-Module2.2-Citizen.postman_collection.json` | Runnable endpoint tests (assertions + auto-chaining). |

## Historical (design log — specifics SUPERSEDED by §6 of CONTEXT)

These predate the 2026-06-16 refactor. They are accurate about *intent and Tier-1 rationale* but their
**IDs (UUID), naming (camelCase), status (full names), endpoint list (incl. issue-dummy) are out of
date** — do not trust those specifics; cross-check against CONTEXT / API-Reference.

- [`Module-2.2-Status-and-Roadmap.md`](./Module-2.2-Status-and-Roadmap.md) — original "where we are / cross-module needs"
- [`Module-2.2-Citizen-DevLog.md`](./Module-2.2-Citizen-DevLog.md) — original development log & rationale
- [`Module-2.2-Tier1-Review-Log.md`](./Module-2.2-Tier1-Review-Log.md) — Tier 1 review-and-refine pass
- [`Module-2.2-Tier1-Presentation-2026-06-12.md`](./Module-2.2-Tier1-Presentation-2026-06-12.md) — Tier 1 standup script

## Where the rest of the context lives

- **Code:** `src/main/java/com/civicdesk/module/citizen/` (31 files — entities, enums, converters,
  repositories, DTOs, services, controllers, exceptions, `support/`).
- **Config/seed:** `src/main/resources/application.properties` (shared), `application-local.properties`
  (local profile), `mock/schema-mock.sql` + `mock/data-mock.sql` (mock users/departments).
