package com.smartinventory.notificationservice.controller;

import com.smartinventory.notificationservice.dto.NotificationApprovalResponseDTO;
import com.smartinventory.notificationservice.dto.NotificationResponseDTO;
import com.smartinventory.notificationservice.dto.PageResponse;
import com.smartinventory.notificationservice.entity.enums.NotificationStatus;
import com.smartinventory.notificationservice.entity.enums.NotificationType;
import com.smartinventory.notificationservice.exception.GlobalExceptionHandler;
import com.smartinventory.notificationservice.exception.ResourceNotFoundException;
import com.smartinventory.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WebMvc slice tests for {@link NotificationController}.
 */
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/notifications - 200 OK")
    void testGetNotifications_Success() throws Exception {
        NotificationResponseDTO dto = new NotificationResponseDTO(
                1L, NotificationType.LOW_STOCK_ALERT, NotificationStatus.PENDING,
                "supplier@example.com", "Low Stock Alert", "Body",
                10L, "PRODUCT", null, null, LocalDateTime.now(), LocalDateTime.now()
        );
        PageResponse<NotificationResponseDTO> pageResponse = new PageResponse<>(
                Collections.singletonList(dto), 0, 20, 1, 1, true
        );

        when(notificationService.getNotifications(any(), any(), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/notifications")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].type").value("LOW_STOCK_ALERT"))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/{id} - 200 OK")
    void testGetNotificationById_Success() throws Exception {
        NotificationResponseDTO dto = new NotificationResponseDTO(
                5L, NotificationType.INVOICE_EMAIL, NotificationStatus.SENT,
                "customer@example.com", "Invoice BILL-100", "Body",
                100L, "BILL", null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );

        when(notificationService.getNotificationById(5L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/notifications/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.recipientEmail").value("customer@example.com"));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/{id} - 404 Not Found")
    void testGetNotificationById_NotFound() throws Exception {
        when(notificationService.getNotificationById(99L))
                .thenThrow(new ResourceNotFoundException("Notification not found with ID: 99"));

        mockMvc.perform(get("/api/v1/notifications/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT /api/v1/notifications/{id}/approve - 200 OK")
    void testApproveNotification_Success() throws Exception {
        NotificationApprovalResponseDTO approvalDTO = new NotificationApprovalResponseDTO(
                12L, NotificationStatus.SENT, "orders@techsource.com", LocalDateTime.now()
        );

        when(notificationService.approveNotification(12L)).thenReturn(approvalDTO);

        mockMvc.perform(put("/api/v1/notifications/12/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(12))
                .andExpect(jsonPath("$.data.status").value("SENT"))
                .andExpect(jsonPath("$.data.recipientEmail").value("orders@techsource.com"));
    }

    @Test
    @DisplayName("PUT /api/v1/notifications/{id}/approve - 400 Bad Request on illegal state")
    void testApproveNotification_IllegalState() throws Exception {
        when(notificationService.approveNotification(12L))
                .thenThrow(new IllegalStateException("Notification must be in PENDING status to approve"));

        mockMvc.perform(put("/api/v1/notifications/12/approve"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Notification must be in PENDING status to approve"));
    }

    @Test
    @DisplayName("POST /api/v1/notifications/{id}/retry - 200 OK")
    void testRetryNotification_Success() throws Exception {
        NotificationResponseDTO dto = new NotificationResponseDTO(
                7L, NotificationType.INVOICE_EMAIL, NotificationStatus.SENT,
                "customer@example.com", "Invoice", "Body",
                50L, "BILL", null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );

        when(notificationService.retryNotification(7L)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/notifications/7/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SENT"));
    }
}
