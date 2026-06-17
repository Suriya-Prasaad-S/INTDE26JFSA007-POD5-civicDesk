# CivicDesk — Module 2.1: Identity & Access Management

Foundation module for CivicDesk. Provides citizen/staff authentication, JWT issuance,
role-based access control, user administration, and audit logging.

## Stack

- Spring Boot 3.4.x + Spring Security 6
- MySQL 8 (runtime) · H2 (tests)
- JWT HS256 (stateless) via jjwt 0.12.3
- BCrypt password hashing (cost 10)
- UUID v4 primary keys (`CHAR(36)`)

> **Note on the framework version:** the project was scaffolded with Spring Boot
> `4.0.6`, but the assignment spec and all provided code target **Spring Boot 3.x /
> Spring Security 6**. `pom.xml` is therefore pinned to `3.4.1` so the documented
> APIs (lambda security DSL, `@EnableMethodSecurity`, jjwt 0.12.3) compile as written.

## Running

```bash
# The civicdesk database is auto-created on first start
# (JDBC URL has ?createDatabaseIfNotExist=true; ddl-auto=update builds the tables).
# Set spring.datasource.username / password in src/main/resources/application.properties, then:
./mvnw spring-boot:run                              # default profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # verbose SQL
```

App starts on `http://localhost:8081/civicDesk` (context path `/civicDesk`).

- **Swagger UI:** http://localhost:8081/civicDesk/swagger-ui.html (use *Authorize* to paste a JWT)
- A default **ADMIN** is seeded on first startup by `DataSeeder` from the
  `ADMIN_EMAIL` / `ADMIN_PASSWORD` / `ADMIN_NAME` / `ADMIN_PHONE` env vars
  (local defaults: `admin@civicdesk.gov` / `Admin@12345`).

### Profiles
- `dev` — `ddl-auto=update`, SQL logging on.
- `prod` — `ddl-auto=validate`, no SQL logging; DB + JWT secret come from env vars.

### Docker
```bash
docker build -t civicdesk-iam .
docker run -p 8081:8081 --env-file .env civicdesk-iam
```

## Tests

```bash
./mvnw test
```

Tests run against in-memory H2 using the `test` profile (`application-test.properties`).

## Project layout

```
com.civicdesk
├── CivicDeskApplication.java
├── config/        SecurityConfig · JwtConfig · CorsConfig · OpenApiConfig · DataSeeder
├── common/
│   ├── exception/  GlobalExceptionHandler + domain exceptions (401/403/404/409/400/423)
│   ├── response/   ApiResponse (unified, with statusCode) · ErrorResponse · PageResponse
│   └── util/       JwtUtil · NationalIdUtil · SecurityContextUtil · ClientIpUtil
├── module/iam/
│   ├── controller/ AuthController · UserController · DepartmentController
│   ├── service/    Auth / User / Department services (interface + impl)
│   ├── repository/  UserRepository · DepartmentRepository
│   ├── entity/     User · Department
│   ├── dto/
│   │   ├── request/   Register · CitizenLogin · StaffLogin · CreateUser · UpdateUserStatus
│   │   └── response/  AuthResponse · UserResponse
│   ├── enums/      Role · UserStatus
│   └── security/   JwtAuthFilter
└── module/auditlog/                         ← self-contained audit-log microservice module
    ├── controller/ AuditLogController         (POST + GET /audit/auditLogs)
    ├── service/    AuditService (interface + impl)
    ├── repository/  AuditLogRepository · spec/AuditLogSpecifications
    ├── entity/     AuditLog
    ├── dto/
    │   ├── request/   CreateAuditLogRequest
    │   └── response/  AuditLogResponse
    ├── enums/      AuditAction · AuditModule  (share with team Day 1)
    ├── validation/ EnumValid + EnumValidator  (reusable "value must be a known enum name")
    └── client/     AuditClient  (other services POST audit entries through this)
```

Teammates' modules (`citizen/`, `servicerequest/`, `permit/`, `grievance/`, `analytics/`)
live as siblings under `module/`. Audit log is already split out this way: `module/auditlog/`
owns the `/audit` prefix and is consumed by IAM (and any module) only through `AuditService`.

## Base URL

Every endpoint is served under the application context path **`/civicDesk`** plus the
IAM module prefix **`/iam`**:

```
http://localhost:8081/civicDesk/iam/...
```

- `/civicDesk` — application-wide context path, shared by **every** module.
- `/iam` — this module's prefix. Other modules own their own (`/audit`, `/grievance`, `/servicerequest`, …),
  so they are **not** trapped under `/iam`. The audit-log module already owns **`/audit`**.

## Roles

`CIT` Citizen · `FO` Field Officer · `DS` Dept. Supervisor · `ENG` Engineer · `CO` Coordinator · `ADM` Admin.
These exact codes are the `@PreAuthorize` and `users.role` contract — do not rename them.

## Endpoints (12)

Paths below are shown after the `/civicDesk` context path. Full URL =
`http://localhost:8081/civicDesk` + path.

| Method | Path                         | Access            | Notes |
|--------|------------------------------|-------------------|-------|
| POST   | `/iam/auth/register`         | Public            | Citizen self-registration → 201 |
| POST   | `/iam/auth/citizen/login`    | Public            | `CIT` only (staff → 403); returns JWT |
| POST   | `/iam/auth/staff/login`      | Public            | Staff only (`CIT` → 403); password-not-set → 403; suspended → 423; returns JWT |
| POST   | `/iam/auth/setPassword`      | Public            | First-time password setup (one-time per account) |
| POST   | `/iam/auth/logout`           | Authenticated     | Audit-logged; client clears the token |
| GET    | `/iam/users/me`              | Authenticated     | Current user's own profile |
| POST   | `/iam/users`                 | `ADM` · `DS`      | `ADM` → creates `DS`; `DS` → creates `FO`/`ENG`/`CO` in own dept |
| GET    | `/iam/users`                 | `ADM` · `DS`      | `ADM`: all users; `DS`: own dept, active only. Filters: `role`, `status`, `departmentId`, `page`, `size` |
| PUT    | `/iam/users/{id}/status`     | `ADM`             | Activate / suspend / deactivate a user |
| POST   | `/audit/auditLogs`           | Authenticated     | Records an audit entry. Body: `userId`, `action`, `module` (enum-validated); IP resolved server-side → 201 |
| GET    | `/audit/auditLogs`           | `ADM` · `CO`      | Newest-first. Filters: `userId`, `action`, `module`, `page`, `size` |
| GET    | `/iam/departments`           | `ADM` · `DS`      | List departments (pick a valid `departmentId`) |

**Example:** list all department supervisors in department `DPT03`:

```
GET http://localhost:8081/civicDesk/iam/users?role=DS&departmentId=DPT03
Authorization: Bearer <admin-jwt>
```

All responses use the standard envelope:
`{ "success", "statusCode", "message", "data", "timestamp" }`.

## Staff onboarding (setPassword on first login)

Admin/supervisor-created accounts are created **without a password**
(`is_password_set = false`). The owner activates the account themselves:

```
Admin creates user (no password)              → is_password_set = false
Staff POST /iam/auth/staff/login              → 403 "Please set your password before logging in"
Staff POST /iam/auth/setPassword              → validates min length, BCrypt-hashes,
   { email, newPassword }                        sets is_password_set = true  (200)
Staff POST /iam/auth/staff/login again        → 200 + JWT
```

`setPassword` works **once** per account (afterwards → 403, use a reset flow).
Citizens (self-register) and the seeded admin already have `is_password_set = true`.

## Departments

`DataSeeder` seeds six departments on startup: **Infrastructure, Public Health,
Licensing & Compliance, Citizen Services, Administration, Compliance & Audit**.
When an `ADM` creates a `DS`, the `departmentId` is **validated against
the departments table** (unknown id → 400) and the department's
`department_supervisor_id` is set to the new supervisor (one-to-one). Use
`GET /iam/departments` to fetch valid department ids.

## Shared with teammates

Hand `Role.java` and `JwtUtil.java` to the other module owners before they start —
the role strings and JWT claim names (`userId`, `role`, `email`) are the cross-module
contract.

## Integration templates for other modules

These work in **any** module folder (`grievance/`, `servicerequest/`, `comment/`,
`license/`, …). The `import` lines use absolute package names, so they are identical
everywhere — only your own `package` line changes per file. The `Authorization: Bearer`
token is validated globally by IAM before your method runs; you never parse it yourself.

### Template 1 — get the current role & userId

```java
import com.civicdesk.common.response.ApiResponse;
import com.civicdesk.common.util.SecurityContextUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

@GetMapping("/example")
@PreAuthorize("hasAnyRole('ADM', 'DS')")   // role codes: CIT/FO/DS/ENG/CO/ADM
public ResponseEntity<ApiResponse> example() {
    String userId = SecurityContextUtil.getCurrentUserId();  // caller's id, e.g. "100023"
    String role   = SecurityContextUtil.getCurrentRole();    // caller's role, e.g. "DS"
    return ResponseEntity.ok(ApiResponse.data(myService.doSomething(userId, role)));
}
```

### Template 2 — audit logging (create / update / delete)

The audit log now lives in its own module (`module/auditlog/`). Other modules write
entries the same way as before — by injecting `AuditService` — but the import is the
new package. The IP is resolved server-side; you never pass it from the client.

```java
import com.civicdesk.common.response.ApiResponse;
import com.civicdesk.common.util.SecurityContextUtil;
import com.civicdesk.common.util.ClientIpUtil;
import com.civicdesk.module.auditlog.enums.AuditAction;
import com.civicdesk.module.auditlog.enums.AuditModule;
import com.civicdesk.module.auditlog.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Inject AuditService via the constructor: private final AuditService auditService;
@PostMapping("/example")
@PreAuthorize("hasRole('CIT')")
public ResponseEntity<ApiResponse> create(
        @Valid @RequestBody CreateExampleRequest req,
        HttpServletRequest httpReq) {                 // inject request to capture client IP
    String userId = SecurityContextUtil.getCurrentUserId();
    myService.create(req, userId);

    // who did what, in which module, from which IP
    auditService.log(userId, AuditAction.CREATE_EXAMPLE.name(),
                     AuditModule.EXAMPLE.name(), ClientIpUtil.resolve(httpReq));

    return ResponseEntity.status(201).body(ApiResponse.of("Created successfully", null));
}
```

> **In-process vs. over HTTP.** Inside the monolith, call `auditService.log(...)` (fire-and-forget,
> returns `void`). When your module is split into its own service and audit becomes a remote
> call, `POST /audit/auditLogs` is the same operation over HTTP — body `{ userId, action, module }`,
> `action`/`module` validated against the enums, IP resolved by the audit service, returns the
> created record (201).
>
> Prefer `AuditAction.X.name()` / `AuditModule.Y.name()` over raw strings — add your
> action/module to `com.civicdesk.module.auditlog.enums` to keep audit values canonical
> across modules (the `POST` endpoint rejects any value not in those enums with a 400).

### Template 3 — audit logging over HTTP (required for separate services)

Because the audit log is now its own module (and on its way to its own deployable service),
**every other service records audit entries by POSTing to `/audit/auditLogs`** rather than
calling an in-process bean. Use the ready-made `AuditClient` — it builds the body, forwards
the caller's JWT, and is best-effort (an audit failure is logged, never thrown, so it cannot
break your business operation).

```java
import com.civicdesk.common.response.ApiResponse;
import com.civicdesk.common.util.SecurityContextUtil;
import com.civicdesk.module.auditlog.client.AuditClient;
import com.civicdesk.module.auditlog.enums.AuditAction;
import com.civicdesk.module.auditlog.enums.AuditModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Inject the client via the constructor: private final AuditClient auditClient;
@PostMapping("/example")
@PreAuthorize("hasRole('CIT')")
public ResponseEntity<ApiResponse> create(
        @Valid @RequestBody CreateExampleRequest req,
        HttpServletRequest httpReq) {
    String userId = SecurityContextUtil.getCurrentUserId();
    myService.create(req, userId);

    // Forward the caller's JWT so the audit service authenticates the same principal.
    String authHeader = httpReq.getHeader(HttpHeaders.AUTHORIZATION);
    auditClient.record(userId, AuditAction.CREATE_EXAMPLE, AuditModule.EXAMPLE, authHeader);

    return ResponseEntity.status(201).body(ApiResponse.of("Created successfully", null));
}
```

Point the client at the audit service with one property (defaults to this app):

```properties
# application.properties of the calling service
app.audit.base-url=http://localhost:8081/civicDesk      # → POST {base-url}/audit/auditLogs
```

> **Which template do I use?** If your code lives *inside* the audit-owning deployment, the
> in-process `AuditService.log(...)` (Template 2) is fine. A **separate** service must use
> `AuditClient` (Template 3) — that is the supported cross-service contract.
