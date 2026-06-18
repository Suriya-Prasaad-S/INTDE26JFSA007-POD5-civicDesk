package com.civicdesk.module.notification.repository;

import com.civicdesk.module.notification.entity.Notification;
import com.civicdesk.module.notification.entity.enums.Category;
import com.civicdesk.module.notification.entity.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    /** All notifications for a user, newest first (fetchNotificationsByUser). */
    List<Notification> findByUserIdOrderByCreatedDateDesc(String userId);

    /** All notifications in a category, newest first (fetchNotificationsByCategory). */
    List<Notification> findByCategoryOrderByCreatedDateDesc(Category category);

    /** All of a user's notifications currently in a given status (used by markAllAsRead). */
    List<Notification> findByUserIdAndStatus(String userId, Status status);

    /** Count of a user's notifications in a given status (fetchUnreadCount). */
    long countByUserIdAndStatus(String userId, Status status);
}
