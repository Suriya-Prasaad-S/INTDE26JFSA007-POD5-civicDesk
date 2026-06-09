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

App starts on `http://localhost:8081`.

- **Swagger UI:** http://localhost:8081/swagger-ui.html (use *Authorize* to paste a JWT)
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
│   └── util/       JwtUtil · NationalIdUtil · SecurityContextUtil
└── module/iam/
    ├── controller/ AuthController · UserController · AuditLogController
    ├── service/    Auth / User / Audit services (interface + impl)
    ├── repository/  UserRepository · AuditLogRepository
    ├── entity/     User · AuditLog
    ├── dto/
    │   ├── request/   Register · CitizenLogin · StaffLogin · CreateUser · UpdateUserStatus
    │   └── response/  AuthResponse · UserResponse · AuditLogResponse
    ├── enums/      Role · UserStatus · AuditAction · AuditModule  (share with team Day 1)
    └── security/   JwtAuthFilter
```



