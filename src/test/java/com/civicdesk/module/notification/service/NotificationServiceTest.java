package com.civicdesk.module.notification.service;

import com.civicdesk.common.exception.ConflictException;
import com.civicdesk.module.notification.dto.request.NotificationRequestDTO;
import com.civicdesk.module.notification.dto.response.NotificationResponseDTO;
import com.civicdesk.module.notification.dto.response.UnreadCountResponse;
import com.civicdesk.module.notification.entity.Notification;
import com.civicdesk.module.notification.entity.enums.Category;
import com.civicdesk.module.notification.entity.enums.Status;
import com.civicdesk.module.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void createNotification_shouldSaveUnreadNotification() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationRequestDTO request = new NotificationRequestDTO(
                "USR-1",
                "Your service request is ready for review.",
                Category.ServiceRequest);

        NotificationResponseDTO response = notificationService.createNotification(request);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();

        assertThat(saved.getNotificationId()).isNotBlank();
        assertThat(saved.getUserId()).isEqualTo("USR-1");
        assertThat(saved.getMessage()).isEqualTo(request.message());
        assertThat(saved.getCategory()).isEqualTo(request.category());
        assertThat(saved.getStatus()).isEqualTo(Status.Unread);

        assertThat(response.notificationId()).isEqualTo(saved.getNotificationId());
        assertThat(response.userId()).isEqualTo(saved.getUserId());
        assertThat(response.status()).isEqualTo(Status.Unread);
    }

    @Test
    void fetchNotificationsByUser_shouldReturnMappedDtos() {
        Notification notification = new Notification();
        notification.setNotificationId("N-1");
        notification.setUserId("USR-1");
        notification.setMessage("Hello");
        notification.setCategory(Category.ServiceRequest);
        notification.setStatus(Status.Unread);

        when(notificationRepository.findByUserIdOrderByCreatedDateDesc("USR-1"))
                .thenReturn(List.of(notification));

        List<NotificationResponseDTO> result = notificationService.fetchNotificationsByUser("USR-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo("USR-1");
        assertThat(result.get(0).message()).isEqualTo("Hello");
        assertThat(result.get(0).category()).isEqualTo(Category.ServiceRequest);
    }

    @Test
    void markAsRead_whenUnread_updatesStatus() {
        Notification notification = new Notification();
        notification.setNotificationId("N-1");
        notification.setUserId("USR-1");
        notification.setMessage("You have a new update.");
        notification.setCategory(Category.ServiceRequest);
        notification.setStatus(Status.Unread);

        when(notificationRepository.findById("N-1")).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(notificationService.markAsRead("N-1").message())
                .isEqualTo("Notification marked as read.");

        assertThat(notification.getStatus()).isEqualTo(Status.Read);
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_whenAlreadyRead_throwsConflictException() {
        Notification notification = new Notification();
        notification.setNotificationId("N-1");
        notification.setUserId("USR-1");
        notification.setMessage("You have a new update.");
        notification.setCategory(Category.ServiceRequest);
        notification.setStatus(Status.Read);

        when(notificationRepository.findById("N-1")).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead("N-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already " + Status.Read);
    }

    @Test
    void fetchUnreadCount_returnsCorrectCount() {
        when(notificationRepository.countByUserIdAndStatus("USR-1", Status.Unread))
                .thenReturn(5L);

        UnreadCountResponse result = notificationService.fetchUnreadCount("USR-1");

        assertThat(result.userId()).isEqualTo("USR-1");
        assertThat(result.unreadCount()).isEqualTo(5L);
    }
}
