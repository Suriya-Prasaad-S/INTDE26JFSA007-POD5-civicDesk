package com.civicdesk.module.notification.controller;

import com.civicdesk.module.notification.dto.request.NotificationRequestDTO;
import com.civicdesk.module.notification.dto.response.MessageResponse;
import com.civicdesk.module.notification.dto.response.NotificationResponseDTO;
import com.civicdesk.module.notification.entity.enums.Category;
import com.civicdesk.module.notification.entity.enums.Status;
import com.civicdesk.module.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @Test
    void createNotification_shouldReturnCreatedNotification() throws Exception {
        NotificationResponseDTO response = new NotificationResponseDTO(
                "N-1",
                "USR-1",
                "Your request has been updated.",
                Category.ServiceRequest,
                Status.Unread,
                LocalDateTime.now());

        when(notificationService.createNotification(any(NotificationRequestDTO.class))).thenReturn(response);

        NotificationRequestDTO request = new NotificationRequestDTO(
                "USR-1",
                "Your request has been updated.",
                Category.ServiceRequest);

        mockMvc.perform(post("/civicDesk/notificationsAlerts/createNotification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notificationId").value("N-1"))
                .andExpect(jsonPath("$.userId").value("USR-1"))
                .andExpect(jsonPath("$.category").value("ServiceRequest"));
    }

    @Test
    void fetchNotificationsByUser_shouldReturnList() throws Exception {
        NotificationResponseDTO notification = new NotificationResponseDTO(
                "N-1",
                "USR-1",
                "Hello",
                Category.ServiceRequest,
                Status.Unread,
                LocalDateTime.now());

        when(notificationService.fetchNotificationsByUser("USR-1")).thenReturn(List.of(notification));

        mockMvc.perform(get("/civicDesk/notificationsAlerts/fetchNotificationsByUser/USR-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notificationId").value("N-1"))
                .andExpect(jsonPath("$[0].userId").value("USR-1"));
    }

    @Test
    void markAsRead_shouldReturnSuccessMessage() throws Exception {
        when(notificationService.markAsRead("N-1"))
                .thenReturn(new MessageResponse("Notification marked as read."));

        mockMvc.perform(put("/civicDesk/notificationsAlerts/markAsRead/N-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification marked as read."));
    }
}
