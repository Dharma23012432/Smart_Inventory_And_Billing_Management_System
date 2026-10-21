package com.smartinventory.notificationservice.service;

import com.smartinventory.notificationservice.dto.NotificationApprovalResponseDTO;
import com.smartinventory.notificationservice.dto.NotificationResponseDTO;
import com.smartinventory.notificationservice.dto.PageResponse;
import com.smartinventory.notificationservice.entity.Notification;
import com.smartinventory.notificationservice.entity.enums.NotificationStatus;
import com.smartinventory.notificationservice.entity.enums.NotificationType;
import com.smartinventory.notificationservice.exception.ResourceNotFoundException;
import com.smartinventory.notificationservice.repository.NotificationRepository;
import com.smartinventory.notificationservice.service.impl.NotificationServiceImpl;
import com.smartinventory.notificationservice.service.mapper.NotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link NotificationServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @Spy
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification pendingAlert;
    private Notification failedEmail;

    @BeforeEach
    void setUp() {
        pendingAlert = new Notification();
        pendingAlert.setId(1L);
        pendingAlert.setType(NotificationType.LOW_STOCK_ALERT);
        pendingAlert.setStatus(NotificationStatus.PENDING);
        pendingAlert.setRecipientEmail("supplier@example.com");
        pendingAlert.setSubject("Low Stock Alert");
        pendingAlert.setBody("Please ship more pens.");
        pendingAlert.setReferenceId(10L);
        pendingAlert.setReferenceType("PRODUCT");

        failedEmail = new Notification();
        failedEmail.setId(2L);
        failedEmail.setType(NotificationType.INVOICE_EMAIL);
        failedEmail.setStatus(NotificationStatus.FAILED);
        failedEmail.setRecipientEmail("customer@example.com");
        failedEmail.setSubject("Invoice BILL-001");
        failedEmail.setBody("Invoice body");
        failedEmail.setErrorMessage("SMTP Connection Timed Out");
        failedEmail.setReferenceId(101L);
        failedEmail.setReferenceType("BILL");
    }

    @Test
    @DisplayName("Get notifications with pagination and status/type filter")
    void testGetNotifications_WithFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(Collections.singletonList(pendingAlert), pageable, 1);

        when(notificationRepository.findByStatusAndType(NotificationStatus.PENDING, NotificationType.LOW_STOCK_ALERT, pageable))
                .thenReturn(page);

        PageResponse<NotificationResponseDTO> response = notificationService.getNotifications(
                NotificationStatus.PENDING, NotificationType.LOW_STOCK_ALERT, pageable);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(1L, response.getContent().get(0).getId());
        assertEquals(NotificationType.LOW_STOCK_ALERT, response.getContent().get(0).getType());
    }

    @Test
    @DisplayName("Get notification by ID successfully")
    void testGetNotificationById_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(pendingAlert));

        NotificationResponseDTO response = notificationService.getNotificationById(1L);
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("supplier@example.com", response.getRecipientEmail());
    }

    @Test
    @DisplayName("Get notification by ID throws ResourceNotFoundException when absent")
    void testGetNotificationById_NotFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.getNotificationById(99L));
    }

    @Test
    @DisplayName("Approve pending low-stock alert successfully")
    void testApproveNotification_Success() throws Exception {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(pendingAlert));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationApprovalResponseDTO response = notificationService.approveNotification(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(NotificationStatus.SENT, response.getStatus());
        assertEquals("supplier@example.com", response.getRecipientEmail());
        assertNotNull(response.getSentAt());

        verify(emailService).sendEmail("supplier@example.com", "Low Stock Alert", "Please ship more pens.");
        verify(notificationRepository).save(pendingAlert);
    }

    @Test
    @DisplayName("Approve notification fails when not in PENDING status")
    void testApproveNotification_InvalidStatus() {
        pendingAlert.setStatus(NotificationStatus.SENT);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(pendingAlert));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> notificationService.approveNotification(1L));
        assertTrue(ex.getMessage().contains("must be in PENDING status"));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Approve notification fails when not a LOW_STOCK_ALERT")
    void testApproveNotification_InvalidType() {
        pendingAlert.setType(NotificationType.INVOICE_EMAIL);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(pendingAlert));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> notificationService.approveNotification(1L));
        assertTrue(ex.getMessage().contains("Only LOW_STOCK_ALERT notifications can be approved"));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Approve notification marks FAILED when email dispatch throws exception")
    void testApproveNotification_EmailFails() throws Exception {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(pendingAlert));
        doThrow(new RuntimeException("SMTP Host Unreachable")).when(emailService)
                .sendEmail(anyString(), anyString(), anyString());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationApprovalResponseDTO response = notificationService.approveNotification(1L);

        assertNotNull(response);
        assertEquals(NotificationStatus.FAILED, response.getStatus());
        assertEquals("SMTP Host Unreachable", pendingAlert.getErrorMessage());
    }

    @Test
    @DisplayName("Retry failed notification successfully")
    void testRetryNotification_Success() throws Exception {
        when(notificationRepository.findById(2L)).thenReturn(Optional.of(failedEmail));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponseDTO response = notificationService.retryNotification(2L);

        assertNotNull(response);
        assertEquals(NotificationStatus.SENT, response.getStatus());
        assertNull(response.getErrorMessage());
        verify(emailService).sendEmail("customer@example.com", "Invoice BILL-001", "Invoice body");
    }

    @Test
    @DisplayName("Retry notification fails when status is not FAILED")
    void testRetryNotification_NotFailed() {
        failedEmail.setStatus(NotificationStatus.SENT);
        when(notificationRepository.findById(2L)).thenReturn(Optional.of(failedEmail));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> notificationService.retryNotification(2L));
        assertTrue(ex.getMessage().contains("Only notifications with FAILED status can be retried"));
    }
}
