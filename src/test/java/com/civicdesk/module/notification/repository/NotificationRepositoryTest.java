package com.civicdesk.module.notification.repository;

import com.civicdesk.module.notification.entity.Notification;
import com.civicdesk.module.notification.entity.enums.Category;
import com.civicdesk.module.notification.entity.enums.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class NotificationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void findByUserIdOrderByCreatedDateDesc_shouldReturnNewestFirst() {
        Notification oldNotification = notification("N-1", "USR-1", "Older", Status.Unread);
        oldNotification.setCreatedDate(LocalDateTime.now().minusDays(1));
        Notification newNotification = notification("N-2", "USR-1", "Newer", Status.Read);
        newNotification.setCreatedDate(LocalDateTime.now());
        entityManager.persist(oldNotification);
        entityManager.persist(newNotification);
        entityManager.flush();
        entityManager.clear();

        List<Notification> result = notificationRepository.findByUserIdOrderByCreatedDateDesc("USR-1");

        assertThat(result).extracting(Notification::getNotificationId).containsExactly("N-2", "N-1");
    }

    @Test
    void countByUserIdAndStatus_shouldCountUnreadNotifications() {
        entityManager.persist(notification("N-1", "USR-1", "First", Status.Unread));
        entityManager.persist(notification("N-2", "USR-1", "Second", Status.Read));
        entityManager.persist(notification("N-3", "USR-2", "Third", Status.Unread));
        entityManager.flush();
        entityManager.clear();

        long count = notificationRepository.countByUserIdAndStatus("USR-1", Status.Unread);

        assertThat(count).isEqualTo(1);
    }

    private Notification notification(String id, String userId, String message, Status status) {
        Notification notification = new Notification();
        notification.setNotificationId(id);
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setCategory(Category.ServiceRequest);
        notification.setStatus(status);
        return notification;
    }
}
