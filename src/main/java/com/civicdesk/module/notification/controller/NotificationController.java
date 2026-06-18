package com.civicdesk.module.notification.controller;

import com.civicdesk.module.notification.dto.request.NotificationRequestDTO;
import com.civicdesk.module.notification.dto.response.MessageResponse;
import com.civicdesk.module.notification.dto.response.NotificationResponseDTO;
import com.civicdesk.module.notification.dto.response.UnreadCountResponse;
import com.civicdesk.module.notification.entity.enums.Category;
import com.civicdesk.module.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for the Notifications &amp; Alerts module (Phase 1 - in-app only).
 * All paths sit under {@code /civicdesk/notificationsAlerts}.
 *
 * <p>{@code POST /createNotification} is designed to be called internally by other CivicDesk
 * services (grievance, permit, service-request, work-order) when a status change occurs, as
 * well as by the frontend. Not-found and invalid-transition checks are enforced here; their
 * 404/409 responses are produced by the shared {@code GlobalExceptionHandler}.</p>
 */
@RestController
@RequestMapping("/civicDesk/notificationsAlerts")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Create a notification for a user (called by other services on a status change). */
    @PostMapping("/createNotification")
    public ResponseEntity<NotificationResponseDTO> createNotification(
            @Valid @RequestBody NotificationRequestDTO request) {
        NotificationResponseDTO response = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** List every notification, newest first. */
    @GetMapping("/fetchAllNotifications")
    public ResponseEntity<List<NotificationResponseDTO>> fetchAllNotifications() {
        return ResponseEntity.ok(notificationService.fetchAllNotifications());
    }

    /** Full details of a single notification by its id. */
    @GetMapping("/fetchNotificationById/{notificationId}")
    public ResponseEntity<NotificationResponseDTO> fetchNotificationById(
            @PathVariable String notificationId) {
        return ResponseEntity.ok(notificationService.fetchNotificationById(notificationId));
    }

    /** All notifications addressed to a specific user, newest first. */
    @GetMapping("/fetchNotificationsByUser/{userId}")
    public ResponseEntity<List<NotificationResponseDTO>> fetchNotificationsByUser(
            @PathVariable String userId) {
        return ResponseEntity.ok(notificationService.fetchNotificationsByUser(userId));
    }

    /** All notifications in a given category, newest first. */
    @GetMapping("/fetchNotificationsByCategory/{category}")
    public ResponseEntity<List<NotificationResponseDTO>> fetchNotificationsByCategory(
            @PathVariable Category category) {
        return ResponseEntity.ok(notificationService.fetchNotificationsByCategory(category));
    }

    /** Count of a user's Unread notifications (for a UI badge). */
    @GetMapping("/fetchUnreadCount/{userId}")
    public ResponseEntity<UnreadCountResponse> fetchUnreadCount(@PathVariable String userId) {
        return ResponseEntity.ok(notificationService.fetchUnreadCount(userId));
    }

    /** Mark a single Unread notification as Read (409 if already Read or Dismissed). */
    @PutMapping("/markAsRead/{notificationId}")
    public ResponseEntity<MessageResponse> markAsRead(@PathVariable String notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId));
    }

    /** Mark all of a user's Unread notifications as Read. */
    @PutMapping("/markAllAsRead/{userId}")
    public ResponseEntity<MessageResponse> markAllAsRead(@PathVariable String userId) {
        return ResponseEntity.ok(notificationService.markAllAsRead(userId));
    }

    /** Dismiss a notification (409 if already Dismissed). */
    @PutMapping("/dismissNotification/{notificationId}")
    public ResponseEntity<MessageResponse> dismissNotification(@PathVariable String notificationId) {
        return ResponseEntity.ok(notificationService.dismissNotification(notificationId));
    }

    /** Permanently delete a notification (404 if it does not exist). */
    @PutMapping("/deleteNotification/{notificationId}")
    public ResponseEntity<MessageResponse> deleteNotification(@PathVariable String notificationId) {
        return ResponseEntity.ok(notificationService.deleteNotification(notificationId));
    }
}
