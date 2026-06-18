package com.civicdesk.module.notification.dto.response;

import com.civicdesk.module.notification.entity.Notification;
import com.civicdesk.module.notification.entity.enums.Category;
import com.civicdesk.module.notification.entity.enums.Status;

import java.time.LocalDateTime;

/**
 * Full representation of a single notification returned to clients.
 */
public record NotificationResponseDTO(
        String notificationId,
        String userId,
        String message,
        Category category,
        Status status,
        LocalDateTime createdDate
) {

    /** Map an entity to its response representation. */
    public static NotificationResponseDTO from(Notification n) {
        return new NotificationResponseDTO(
                n.getNotificationId(),
                n.getUserId(),
                n.getMessage(),
                n.getCategory(),
                n.getStatus(),
                n.getCreatedDate());
    }
}
