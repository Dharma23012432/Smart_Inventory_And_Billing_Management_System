package com.smartinventory.notificationservice.event;

import com.smartinventory.notificationservice.entity.Bill;
import com.smartinventory.notificationservice.entity.Notification;
import com.smartinventory.notificationservice.entity.enums.NotificationStatus;
import com.smartinventory.notificationservice.entity.enums.NotificationType;
import com.smartinventory.notificationservice.repository.BillRepository;
import com.smartinventory.notificationservice.repository.NotificationRepository;
import com.smartinventory.notificationservice.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EventConsumer}.
 */
@ExtendWith(MockitoExtension.class)
class EventConsumerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private BillRepository billRepository;

    @Mock
    private com.smartinventory.notificationservice.repository.ProductRepository productRepository;

    @InjectMocks
    private EventConsumer eventConsumer;

    private PaymentCompletedEvent paymentCompletedEvent;

    @BeforeEach
    void setUp() {
        paymentCompletedEvent = PaymentCompletedEvent.builder()
                .paymentId(1L)
                .billId(100L)
                .billNumber("BILL-20261019-0001")
                .customerId(10L)
                .customerName("Jane Smith")
                .customerEmail("jane.smith@example.com")
                .amountPaid(new BigDecimal("250.00"))
                .paymentMethod("UPI")
                .paymentDate(LocalDateTime.now())
                .eventTimestamp(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Successfully updates existing INVOICE_EMAIL notification and immediately sends email to customer")
    void testHandlePaymentCompleted_UpdatesExistingInvoiceEmail() throws Exception {
        Notification existingNotification = new Notification();
        existingNotification.setId(42L);
        existingNotification.setType(NotificationType.INVOICE_EMAIL);
        existingNotification.setStatus(NotificationStatus.PENDING);
        existingNotification.setRecipientEmail("jane.smith@example.com");
        existingNotification.setReferenceId(100L);
        existingNotification.setReferenceType("BILL");
        existingNotification.setSubject("Invoice - BILL-20261019-0001");
        existingNotification.setBody("Draft invoice awaiting payment");

        Bill bill = new Bill();
        bill.setId(100L);
        bill.setBillNumber("BILL-20261019-0001");
        bill.setInvoiceSent(false);

        when(notificationRepository.findFirstByReferenceTypeAndReferenceIdAndTypeOrderByCreatedAtDesc(
                "BILL", 100L, NotificationType.INVOICE_EMAIL))
                .thenReturn(Optional.of(existingNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(billRepository.findById(100L)).thenReturn(Optional.of(bill));

        eventConsumer.handlePaymentCompleted(paymentCompletedEvent);

        // Verify email was dispatched immediately to customer without manual approval
        verify(emailService, times(1)).sendEmail(
                eq("jane.smith@example.com"),
                eq("Invoice - BILL-20261019-0001"),
                contains("Tax Invoice & Payment Receipt")
        );

        // Verify bill.invoiceSent updated to true
        assertTrue(bill.getInvoiceSent());
        verify(billRepository, times(1)).save(bill);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertNotNull(saved);
        assertEquals(42L, saved.getId()); // Confirms the SAME existing record was updated
        assertEquals(NotificationType.INVOICE_EMAIL, saved.getType());
        assertEquals(NotificationStatus.SENT, saved.getStatus());
        assertNotNull(saved.getSentAt());
        assertNull(saved.getErrorMessage());
        assertEquals("BILL", saved.getReferenceType());
        assertEquals(100L, saved.getReferenceId());
        assertEquals("jane.smith@example.com", saved.getRecipientEmail());
        assertEquals("Invoice - BILL-20261019-0001", saved.getSubject());

        // Verify body contains required fields
        assertTrue(saved.getBody().contains("Jane Smith"));
        assertTrue(saved.getBody().contains("BILL-20261019-0001"));
        assertTrue(saved.getBody().contains("250.00"));
        assertTrue(saved.getBody().contains("UPI"));
        assertTrue(saved.getBody().contains("#1"));
        assertTrue(saved.getBody().contains("PAID / COMPLETED"));
        assertTrue(saved.getBody().contains("Items Summary"));
        assertTrue(saved.getBody().contains("Subtotal"));
        assertTrue(saved.getBody().contains("Discount"));
        assertTrue(saved.getBody().contains("Tax"));
        assertTrue(saved.getBody().contains("Total Amount"));
    }

    @Test
    @DisplayName("Marks notification as FAILED and retains record when email sending throws exception")
    void testHandlePaymentCompleted_EmailSendingFailure() throws Exception {
        Notification existingNotification = new Notification();
        existingNotification.setId(42L);
        existingNotification.setType(NotificationType.INVOICE_EMAIL);
        existingNotification.setStatus(NotificationStatus.PENDING);
        existingNotification.setRecipientEmail("jane.smith@example.com");

        when(notificationRepository.findFirstByReferenceTypeAndReferenceIdAndTypeOrderByCreatedAtDesc(
                "BILL", 100L, NotificationType.INVOICE_EMAIL))
                .thenReturn(Optional.of(existingNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new RuntimeException("SMTP connection timed out")).when(emailService)
                .sendEmail(any(), any(), any());

        eventConsumer.handlePaymentCompleted(paymentCompletedEvent);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertNotNull(saved);
        assertEquals(NotificationStatus.FAILED, saved.getStatus());
        assertEquals("SMTP connection timed out", saved.getErrorMessage());
        assertNull(saved.getSentAt());
    }

    @Test
    @DisplayName("Creates fallback INVOICE_EMAIL and immediately sends email when no existing draft invoice is found")
    void testHandlePaymentCompleted_FallbackWhenNoExistingInvoice() throws Exception {
        when(notificationRepository.findFirstByReferenceTypeAndReferenceIdAndTypeOrderByCreatedAtDesc(
                "BILL", 100L, NotificationType.INVOICE_EMAIL))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        eventConsumer.handlePaymentCompleted(paymentCompletedEvent);

        verify(emailService, times(1)).sendEmail(
                eq("jane.smith@example.com"),
                eq("Invoice - BILL-20261019-0001"),
                contains("Tax Invoice & Payment Receipt")
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertNotNull(saved);
        assertEquals(NotificationType.INVOICE_EMAIL, saved.getType());
        assertEquals(NotificationStatus.SENT, saved.getStatus());
        assertEquals("BILL", saved.getReferenceType());
        assertEquals(100L, saved.getReferenceId());
        assertEquals("jane.smith@example.com", saved.getRecipientEmail());
    }

    @Test
    @DisplayName("Ignores null or invalid PaymentCompletedEvent gracefully")
    void testHandlePaymentCompleted_NullEvent() {
        eventConsumer.handlePaymentCompleted(null);
        verify(notificationRepository, never()).save(any());

        PaymentCompletedEvent emptyEvent = new PaymentCompletedEvent();
        eventConsumer.handlePaymentCompleted(emptyEvent);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Successfully consumes BillConfirmedEvent and persists draft INVOICE_EMAIL notification without sending email")
    void testHandleBillConfirmed_Success() throws Exception {
        BillConfirmedEvent billConfirmedEvent = BillConfirmedEvent.builder()
                .billId(100L)
                .billNumber("BILL-20261019-0001")
                .customerId(10L)
                .customerName("Jane Smith")
                .customerEmail("jane.smith@example.com")
                .totalAmount(new BigDecimal("250.00"))
                .billDate(LocalDate.of(2026, 10, 19))
                .build();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        eventConsumer.handleBillConfirmed(billConfirmedEvent);

        // Verify NO email is sent upon BillConfirmed
        verify(emailService, never()).sendEmail(any(), any(), any());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertNotNull(saved);
        assertEquals(NotificationType.INVOICE_EMAIL, saved.getType());
        assertEquals(NotificationStatus.PENDING, saved.getStatus());
        assertEquals("BILL", saved.getReferenceType());
        assertEquals(100L, saved.getReferenceId());
        assertEquals("jane.smith@example.com", saved.getRecipientEmail());
        assertEquals("Invoice - BILL-20261019-0001", saved.getSubject());
        assertTrue(saved.getBody().contains("Jane Smith"));
        assertTrue(saved.getBody().contains("250.00"));
    }

    @Test
    @DisplayName("Successfully consumes LowStockDetectedEvent and creates PENDING LOW_STOCK_ALERT notification without sending email")
    void testHandleLowStockDetected_Success() throws Exception {
        LowStockDetectedPayload payload = new LowStockDetectedPayload(
                5L, "Logitech Wireless Mouse", "LOGI-M280", 4, 10, 2L, "TechSource Distribution Ltd", "orders@techsource.com"
        );
        LowStockDetectedEvent event = new LowStockDetectedEvent("evt-123", "LOW_STOCK_DETECTED", LocalDateTime.now(), payload);

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        eventConsumer.handleLowStockDetected(event);

        // Verify NO email is sent automatically (awaits approval endpoint)
        verify(emailService, never()).sendEmail(any(), any(), any());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertNotNull(saved);
        assertEquals(NotificationType.LOW_STOCK_ALERT, saved.getType());
        assertEquals(NotificationStatus.PENDING, saved.getStatus());
        assertEquals("PRODUCT", saved.getReferenceType());
        assertEquals(5L, saved.getReferenceId());
        assertEquals("orders@techsource.com", saved.getRecipientEmail());
        assertEquals("Purchase Request - Low Stock", saved.getSubject());
        assertTrue(saved.getBody().contains("TechSource Distribution Ltd"));
        assertTrue(saved.getBody().contains("Logitech Wireless Mouse"));
        assertTrue(saved.getBody().contains("LOGI-M280"));
        assertTrue(saved.getBody().contains("4 units"));
        assertTrue(saved.getBody().contains("10 units"));
        assertTrue(saved.getBody().contains("Requested Quantity"));
        assertTrue(saved.getBody().contains("Request Date"));
    }
}
