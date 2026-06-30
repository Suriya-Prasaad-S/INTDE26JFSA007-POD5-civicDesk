# Notification Module — Integration Guide

This document explains the Notification module's public API, DTOs, repository contract, entity shape, integration points, test guidance and configuration so other modules (for example Service Request) can wire to it correctly.

## Overview

- Purpose: provide in-app notifications for citizens when domain events occur (service request state changes, approvals, rejections, etc.).
- Location: module code under `src/main/java/com/civicdesk/module/notification`.
- Important classes:
  - Service: `NotificationService`
  - Repository: `NotificationRepository`
  - Entity: `Notification` (JPA)
  - Controller (REST endpoints): `NotificationController` (tests reference the REST paths used below)

## Entity (summary)

`Notification` fields (important):

- `notificationId` (String, PK)
- `userId` (String) — ID owned by IAM module; stored as a plain string
- `message` (TEXT)
- `category` (enum `Category`)
- `status` (enum `Status`) — defaults to `Unread`
- `createdDate` (LocalDateTime) — annotated with `@CreationTimestamp`

Note: `createdDate` is set by Hibernate when persisted; tests sometimes set it directly for deterministic ordering.

## Repository contract

`NotificationRepository` extends `JpaRepository<Notification,String>` and exposes the derived queries used by the service:

- `List<Notification> findByUserIdOrderByCreatedDateDesc(String userId)` — fetch user's notifications newest-first
- `List<Notification> findByCategoryOrderByCreatedDateDesc(Category category)`
- `List<Notification> findByUserIdAndStatus(String userId, Status status)`
- `long countByUserIdAndStatus(String userId, Status status)`

When writing custom queries that rely on `createdDate` ordering, prefer the repository methods above so pagination and sorting are consistent.

## Service API (programmatic)

`NotificationService` exposes the following useful methods for other modules to call:

- `NotificationResponseDTO createNotification(NotificationRequestDTO request)` — create and persist a new notification (sets status `Unread`).
- `List<NotificationResponseDTO> fetchNotificationsByUser(String userId)`
- `List<NotificationResponseDTO> fetchNotificationsByCategory(Category category)`
- `UnreadCountResponse fetchUnreadCount(String userId)`
- `MessageResponse markAsRead(String notificationId)`
- `MessageResponse markAllAsRead(String userId)`
- `MessageResponse dismissNotification(String notificationId)`
- `MessageResponse deleteNotification(String notificationId)`

Typical usage from another service (constructor injection):

```java
private final NotificationService notificationService;

public SomeService(NotificationService notificationService) {
    this.notificationService = notificationService;
}

// creating a notification
notificationService.createNotification(new NotificationRequestDTO(
        userId,
        "Your service request REQ-123 has been submitted",
        Category.ServiceRequest));
```

Imports:

```java
import com.civicdesk.module.notification.dto.request.NotificationRequestDTO;
import com.civicdesk.module.notification.entity.enums.Category;
```

## REST endpoints (controller)

The test-suite references the following controller paths — use them when integrating or testing against a running instance:

- POST `/civicDesk/notificationsAlerts/createNotification` — body: `NotificationRequestDTO` → returns `NotificationResponseDTO` (HTTP 201)
- GET `/civicDesk/notificationsAlerts/fetchNotificationsByUser/{userId}` — returns list of `NotificationResponseDTO`
- PUT `/civicDesk/notificationsAlerts/markAsRead/{notificationId}` — marks single notification as Read and returns `MessageResponse`

These endpoints are convenience wrappers around the service API. If a calling module is in the same JVM, prefer calling `NotificationService` directly.

## DTOs (summary)

- `NotificationRequestDTO` — fields: `userId`, `message`, `category` (enum)
- `NotificationResponseDTO` — fields: `notificationId`, `userId`, `message`, `category`, `status`, `createdDate`
- `UnreadCountResponse` — `userId`, `unreadCount`
- `MessageResponse` — `message` (used for simple acknowledgements)

## Enums

- `Category` — categories the module supports (e.g., ServiceRequest)
- `Status` — `Unread`, `Read`, `Dismissed`

## Integration notes / pitfalls

- Dummy data seeding: the repository `DummyDataSeeder` in the serviceRequest module seeds sample `service_catalog` rows (e.g. `svc-0001`) and other placeholder rows when the application starts. When running repository slice tests, tests should use an embedded DB (or `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)`) to avoid picking up those seeded rows.
- Ordering: `findByUserIdOrderByCreatedDateDesc(...)` relies on `createdDate`. In integration/DB tests the column is set by Hibernate; in unit tests where `Notification` is constructed manually, set `createdDate` explicitly if deterministic ordering matters.
- `createdDate` uses `@CreationTimestamp` so manual setting in production code is not necessary.

## Error handling and status transitions

- `createNotification` sets `status` to `Unread` when persisted.
- `markAsRead` throws `ConflictException` if the notification is not `Unread`.
- `dismissNotification` throws `ConflictException` if already dismissed.

Other modules that create notifications should treat `createNotification` as fire-and-forget: it returns the saved DTO and throws only for unexpected failures.

## Testing the Notification module

- Run only notification repository tests:

```powershell
.\mvnw.cmd -Dtest=NotificationRepositoryTest test
```

- Run all notification-related tests (naming pattern):

```powershell
.\mvnw.cmd -Dtest=*Notification* test
```

- Notes:
  - Use `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)` on `@DataJpaTest` slices to force H2 and avoid shared MySQL seed interference.
  - When asserting order by `createdDate` in slice tests, either let Hibernate set the timestamp on persist or explicitly call `setCreatedDate(...)` before persisting if tests need precise control.

## Configuration

- The module respects `spring.jpa.*` and datasource configuration in `application.properties`.
- Logging: `logging.level.com.civicdesk.module.notification=DEBUG` can be enabled to trace repository and service operations.

## Example payloads

Create request JSON (POST):

```json
{
  "userId": "USR-1",
  "message": "Your request REQ-123 has been submitted.",
  "category": "ServiceRequest"
}
```

Create response (example):

```json
{
  "notificationId": "N-1",
  "userId": "USR-1",
  "message": "Your request REQ-123 has been submitted.",
  "category": "ServiceRequest",
  "status": "Unread",
  "createdDate": "2026-06-30T09:00:00"
}
```

## Quick checklist for other module implementers

- Add `NotificationService` as a constructor dependency and call `createNotification(...)` at points you want a citizen notified.
- Use `Category.ServiceRequest` (or other categories defined in the `notification` module) when creating notifications.
- For REST integrations, use the controller endpoints above; for in-process calls prefer direct service calls.
- When writing integration tests that include the DB, ensure tests run against an embedded DB or isolate MySQL test data to avoid interfering with seeded rows.

---

See `docs/examples.md` for concrete example consumer code and REST request snippets.
