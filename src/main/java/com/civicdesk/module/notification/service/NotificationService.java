package com.civicdesk.module.notification.service;

import com.civicdesk.common.exception.ConflictException;
import com.civicdesk.common.exception.ResourceNotFoundException;
import com.civicdesk.module.notification.dto.request.NotificationRequestDTO;
import com.civicdesk.module.notification.dto.response.MessageResponse;
import com.civicdesk.module.notification.dto.response.NotificationResponseDTO;
import com.civicdesk.module.notification.dto.response.UnreadCountResponse;
import com.civicdesk.module.notification.entity.Notification;
import com.civicdesk.module.notification.entity.enums.Category;
import com.civicdesk.module.notification.entity.enums.Status;
import com.civicdesk.module.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for the Notifications &amp; Alerts module (Phase 1 - in-app only).
 *
 * <p>Notifications are created by other CivicDesk services on status changes and are read,
 * marked and dismissed by the owning citizen. Status transitions are guarded: a notification
 * may only be read while it is {@code Unread}, and only dismissed while it is not already
 * {@code Dismissed}. Violations surface as 409 Conflict.</p>
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /** Create a new notification for a user (createNotification). Callable by other services. */
    public NotificationResponseDTO createNotification(NotificationRequestDTO request) {
        Notification notification = new Notification();
        notification.setNotificationId(UUID.randomUUID().toString());
        notification.setUserId(request.userId());
        notification.setMessage(request.message());
        notification.setCategory(request.category());
        notification.setStatus(Status.Unread);

        Notification saved = notificationRepository.save(notification);
        return NotificationResponseDTO.from(saved);
    }

    /** All notifications, newest first (fetchAllNotifications). */
    public List<NotificationResponseDTO> fetchAllNotifications() {
        return notificationRepository.findAll().stream()
                .map(NotificationResponseDTO::from)
                .toList();
    }

    /** A single notification by id (fetchNotificationById). */
    public NotificationResponseDTO fetchNotificationById(String notificationId) {
        return NotificationResponseDTO.from(getOrThrow(notificationId));
    }

    /** All notifications for a user, newest first (fetchNotificationsByUser). */
    public List<NotificationResponseDTO> fetchNotificationsByUser(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedDateDesc(userId).stream()
                .map(NotificationResponseDTO::from)
                .toList();
    }

    /** All notifications in a category, newest first (fetchNotificationsByCategory). */
    public List<NotificationResponseDTO> fetchNotificationsByCategory(Category category) {
        return notificationRepository.findByCategoryOrderByCreatedDateDesc(category).stream()
                .map(NotificationResponseDTO::from)
                .toList();
    }

    /** Number of Unread notifications for a user (fetchUnreadCount). */
    public UnreadCountResponse fetchUnreadCount(String userId) {
        long count = notificationRepository.countByUserIdAndStatus(userId, Status.Unread);
        return new UnreadCountResponse(userId, count);
    }

    /** Mark a single Unread notification as Read (markAsRead). */
    public MessageResponse markAsRead(String notificationId) {
        Notification notification = getOrThrow(notificationId);

        if (notification.getStatus() != Status.Unread) {
            throw new ConflictException(
                    "Notification cannot be marked as read because it is already "
                            + notification.getStatus() + ".");
        }

        notification.setStatus(Status.Read);
        notificationRepository.save(notification);
        return new MessageResponse("Notification marked as read.");
    }

    /** Mark every Unread notification for a user as Read (markAllAsRead). */
    public MessageResponse markAllAsRead(String userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndStatus(userId, Status.Unread);

        for (Notification notification : unread) {
            notification.setStatus(Status.Read);
        }
        notificationRepository.saveAll(unread);

        return new MessageResponse(unread.size() + " notification(s) marked as read.");
    }

    /** Dismiss a notification that has not already been dismissed (dismissNotification). */
    public MessageResponse dismissNotification(String notificationId) {
        Notification notification = getOrThrow(notificationId);

        if (notification.getStatus() == Status.Dismissed) {
            throw new ConflictException("Notification has already been dismissed.");
        }

        notification.setStatus(Status.Dismissed);
        notificationRepository.save(notification);
        return new MessageResponse("Notification dismissed.");
    }

    /** Permanently delete a notification (deleteNotification). */
    public MessageResponse deleteNotification(String notificationId) {
        Notification notification = getOrThrow(notificationId);
        notificationRepository.delete(notification);
        return new MessageResponse("Notification deleted.");
    }

    private Notification getOrThrow(String notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found. No notification exists with the given notificationId."));
    }
}
