# CivicDesk — `main` (project-structure template)

This is the **shared main branch**: a project-structure template only — **no business
code**. Nobody commits feature code directly to `main`. Each module owner creates their
own branch, fills in their module folder using this layout, and merges back.

> The folders are kept by empty `.gitkeep` placeholders. Delete the placeholder once you
> add real files to a folder.

## Folder structure

```
civicdesk-main/
├── pom.xml                     # shared build + dependencies (the common build file)
├── mvnw, mvnw.cmd, .mvn/       # Maven wrapper
├── Dockerfile, .gitignore
└── src/
    ├── main/
    │   ├── java/com/civicdesk/
    │   │   ├── config/                 # shared @Configuration (security, cors, openapi, …)
    │   │   ├── common/                 # cross-module shared building blocks
    │   │   │   ├── exception/          #   global handler + shared exception types
    │   │   │   ├── response/           #   shared response envelopes
    │   │   │   └── util/               #   shared utilities (jwt, security context, …)
    │   │   └── module/                 # ONE folder per module — owners fill these in
    │   │       ├── iam/                # Module 2.1 — Suriya
    │   │       ├── citizen/            # Module 2.2 — Pruthiviraj
    │   │       ├── servicerequest/     # Module 2.3 — Haresh
    │   │       ├── permit/             # Module 2.4 — Amirtha
    │   │       ├── grievance/          # Module 2.5 — Anand
    │   │       └── analytics/          # Module 2.7 — Suriya
    │   └── resources/
    │       └── application.properties  # config template (placeholders, no secrets)
    └── test/
        └── java/com/civicdesk/module/<module>/{controller,service,repository,integration}/
```

### Standard layers inside every module
Each `module/<name>/` folder follows the same layout:

```
module/<name>/
├── controller/      # @RestController — HTTP endpoints
├── service/         # interfaces + @Service implementations (business logic)
├── repository/      # Spring Data @Repository interfaces
├── entity/          # @Entity JPA classes
└── dto/
    ├── request/     # request bodies
    └── response/    # response payloads
```

And the matching test layout under `src/test/java/com/civicdesk/module/<name>/`:
`controller/` · `service/` · `repository/` · `integration/`.

## How a teammate uses this

1. Create your branch off `main`.
2. Work **only** inside your `module/<name>/...` folders, plus add genuinely shared
   pieces under `config/` or `common/` (agree on those with the team first).
3. Beans are auto-detected — the app component-scans `com.civicdesk`, so a
   `@RestController` / `@Service` / `@Entity` you add is picked up with no extra wiring.
4. Put tests in the mirrored `src/test/.../module/<name>/...` folders.
5. Open a PR back into `main`.

## Notes
- `main` intentionally has **no application bootstrap class and no module code** — those
  arrive as modules are merged in. `./mvnw compile` works on the empty skeleton;
  `./mvnw package`/`spring-boot:run` only work once a `@SpringBootApplication` class exists.
- Keep secrets out of the shared `application.properties`; use environment variables on
  your branch/deploy.
- Stack: Java 21, Spring Boot 3.4.x, Spring Security 6 (versions pinned in `pom.xml`).

## Module documentation
- The Notification module integration guide is in `docs/notification-module.md`.
- Concrete consumer examples are in `docs/examples.md`.
