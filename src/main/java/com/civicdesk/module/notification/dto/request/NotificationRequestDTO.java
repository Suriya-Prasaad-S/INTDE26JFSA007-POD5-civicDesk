package com.civicdesk.module.notification.dto.request;

import com.civicdesk.module.notification.entity.enums.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Body for POST /civicdesk/notificationsAlerts/createNotification.
 *
 * <p>Designed to be posted by other CivicDesk services (grievance, permit, service-request,
 * work-order) when a status change occurs, as well as by any internal caller. The recipient
 * ({@code userId}), the human-readable {@code message} and the {@code category} are supplied;
 * the id, the initial {@code Unread} status and {@code createdDate} are set by the service.</p>
 */
public record NotificationRequestDTO(

        @NotBlank(message = "userId is required")
        String userId,

        @NotBlank(message = "message is required")
        String message,

        @NotNull(message = "category is required and must be one of: ServiceRequest, Permit, Grievance, WorkOrder, Compliance")
        Category category
) {
}
