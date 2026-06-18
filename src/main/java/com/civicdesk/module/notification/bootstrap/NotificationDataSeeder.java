package com.civicdesk.module.notification.bootstrap;

import com.civicdesk.module.notification.entity.Notification;
import com.civicdesk.module.notification.entity.enums.Category;
import com.civicdesk.module.notification.entity.enums.Status;
import com.civicdesk.module.notification.repository.NotificationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * TEMPORARY startup seeder for the Notifications service — the Java equivalent of a {@code data.sql}.
 *
 * <p>Populates the {@code notification} table with sample rows spanning every category and
 * status for a couple of citizens, so the fetch / unread-count / mark / dismiss endpoints can
 * be exercised end-to-end before the upstream services start posting real notifications. The
 * {@code userId}s mirror the citizens seeded by the Service Request module.</p>
 *
 * <p>Idempotent: it only seeds when the table is empty, so restarts are safe.
 * <b>Delete this class once real notifications flow in from the other services.</b></p>
 */
@Component
public class NotificationDataSeeder implements CommandLineRunner {

    private static final String USER_MEENA = "usr-c001";
    private static final String USER_SURESH = "usr-c002";

    private final NotificationRepository notificationRepository;

    public NotificationDataSeeder(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void run(String... args) {
        if (notificationRepository.count() > 0) {
            return;
        }

        notificationRepository.saveAll(List.of(
                // Meena (usr-c001): a mix of statuses across categories.
                notification(USER_MEENA, "Your Birth Certificate request SR-1001 has been submitted.",
                        Category.ServiceRequest, Status.Read),
                notification(USER_MEENA, "An officer has been assigned to your request SR-1001.",
                        Category.ServiceRequest, Status.Unread),
                notification(USER_MEENA, "Your building permit PRM-2001 is now under review.",
                        Category.Permit, Status.Unread),
                notification(USER_MEENA, "Your grievance GRV-3001 has been resolved.",
                        Category.Grievance, Status.Dismissed),
                notification(USER_MEENA, "Annual property compliance check is due in 30 days.",
                        Category.Compliance, Status.Unread),

                // Suresh (usr-c002): work order and permit updates.
                notification(USER_SURESH, "Work order WO-4001 for your locality has been scheduled.",
                        Category.WorkOrder, Status.Unread),
                notification(USER_SURESH, "Work order WO-4001 has been completed.",
                        Category.WorkOrder, Status.Read),
                notification(USER_SURESH, "Your trade license permit PRM-2002 has been approved.",
                        Category.Permit, Status.Unread),
                notification(USER_SURESH, "Your Income Certificate request SR-1002 needs additional documents.",
                        Category.ServiceRequest, Status.Unread)));
    }

    private Notification notification(String userId, String message, Category category, Status status) {
        Notification notification = new Notification();
        notification.setNotificationId(UUID.randomUUID().toString());
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setCategory(category);
        notification.setStatus(status);
        return notification;
    }
}