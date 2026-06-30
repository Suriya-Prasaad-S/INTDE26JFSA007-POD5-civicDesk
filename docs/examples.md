# Notification Module Examples

This file provides example consumer code and REST requests that other modules can use when integrating with the Notification module.

## Java consumer example: `NotificationService`

Use constructor injection to get `NotificationService` in the dependent service.

```java
import com.civicdesk.module.notification.dto.request.NotificationRequestDTO;
import com.civicdesk.module.notification.entity.enums.Category;
import com.civicdesk.module.notification.service.NotificationService;

@Service
public class SomeBusinessService {

    private final NotificationService notificationService;

    public SomeBusinessService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void notifyUserAboutRequest(String userId, String requestId) {
        String message = "Your service request " + requestId + " has been submitted successfully.";

        notificationService.createNotification(new NotificationRequestDTO(
                userId,
                message,
                Category.ServiceRequest));
    }
}
```

### Example usage from `ServiceRequestService`

When a service request is submitted or its status changes, call `createNotification(...)` with the citizen's `userId`.

```java
notificationService.createNotification(new NotificationRequestDTO(
        citizen.getUserId(),
        "Your service request " + serviceRequest.getRequestId() + " is now under review.",
        Category.ServiceRequest));
```

## Java consumer example: mark notification read

```java
MessageResponse response = notificationService.markAsRead(notificationId);
```

If the notification is already read or dismissed, the service throws `ConflictException`.

## REST examples

### Create a notification

```bash
curl -X POST \
  http://localhost:8082/civicDesk/notificationsAlerts/createNotification \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USR-1",
    "message": "Your service request REQ-123 has been submitted.",
    "category": "ServiceRequest"
  }'
```

Expected response:

```json
{
  "notificationId": "N-1",
  "userId": "USR-1",
  "message": "Your service request REQ-123 has been submitted.",
  "category": "ServiceRequest",
  "status": "Unread",
  "createdDate": "2026-06-30T09:00:00"
}
```

### Fetch notifications by user

```bash
curl http://localhost:8082/civicDesk/notificationsAlerts/fetchNotificationsByUser/USR-1
```

### Mark a notification as read

```bash
curl -X PUT http://localhost:8082/civicDesk/notificationsAlerts/markAsRead/N-1
```

## Recommended integration pattern

- Prefer direct `NotificationService` calls from within the same JVM, because it avoids network overhead and keeps the contract type-safe.
- Use REST only when the caller is in a different service or deployment boundary.
- For tests, mock `NotificationService` in unit tests and use the repository or slice tests for persistence behavior.
