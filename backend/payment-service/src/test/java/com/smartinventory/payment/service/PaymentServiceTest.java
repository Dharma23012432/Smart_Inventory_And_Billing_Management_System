package com.smartinventory.payment.service;

import com.smartinventory.payment.dto.PaymentRequestDTO;
import com.smartinventory.payment.dto.PaymentResponseDTO;
import com.smartinventory.payment.entity.Bill;
import com.smartinventory.payment.entity.Customer;
import com.smartinventory.payment.entity.Payment;
import com.smartinventory.payment.entity.enums.BillStatus;
import com.smartinventory.payment.entity.enums.PaymentMethod;
import com.smartinventory.payment.entity.enums.PaymentStatus;
import com.smartinventory.payment.event.EventPublisher;
import com.smartinventory.payment.event.PaymentCompletedEvent;
import com.smartinventory.payment.exception.DuplicateResourceException;
import com.smartinventory.payment.exception.ResourceNotFoundException;
import com.smartinventory.payment.repository.BillRepository;
import com.smartinventory.payment.repository.PaymentRepository;
import com.smartinventory.payment.service.impl.PaymentServiceImpl;
import com.smartinventory.payment.service.mapper.PaymentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PaymentServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BillRepository billRepository;

    @Spy
    private PaymentMapper paymentMapper;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Bill testBill;
    private Customer testCustomer;
    private PaymentRequestDTO paymentRequest;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setId(10L);
        testCustomer.setName("Jane Smith");
        testCustomer.setEmail("jane@example.com");
        testCustomer.setPhone("9876543210");

        testBill = new Bill();
        testBill.setId(100L);
        testBill.setBillNumber("BILL-20261019-0001");
        testBill.setStatus(BillStatus.CONFIRMED);
        testBill.setTotalAmount(new BigDecimal("250.00"));
        testBill.setCustomer(testCustomer);

        paymentRequest = new PaymentRequestDTO();
        paymentRequest.setBillId(100L);
        paymentRequest.setAmountPaid(new BigDecimal("250.00"));
        paymentRequest.setPaymentMethod(PaymentMethod.UPI);
        paymentRequest.setReferenceNumber("UPI-TXN-12345");
    }

    @Test
    @DisplayName("Process payment successfully for a confirmed bill")
    void testProcessPayment_Success() {
        when(billRepository.findById(100L)).thenReturn(Optional.of(testBill));
        when(paymentRepository.existsByBillId(100L)).thenReturn(false);

        Payment savedPayment = new Payment();
        savedPayment.setId(1L);
        savedPayment.setBill(testBill);
        savedPayment.setAmountPaid(new BigDecimal("250.00"));
        savedPayment.setPaymentMethod(PaymentMethod.UPI);
        savedPayment.setReferenceNumber("UPI-TXN-12345");
        savedPayment.setStatus(PaymentStatus.COMPLETED);
        savedPayment.setPaymentDate(LocalDateTime.now());

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(billRepository.save(any(Bill.class))).thenReturn(testBill);

        PaymentResponseDTO response = paymentService.processPayment(paymentRequest);

        assertNotNull(response);
        assertEquals(1L, response.getPaymentId());
        assertEquals(100L, response.getBillId());
        assertEquals("BILL-20261019-0001", response.getBillNumber());
        assertEquals(new BigDecimal("250.00"), response.getAmountPaid());
        assertEquals(PaymentMethod.UPI, response.getPaymentMethod());
        assertEquals("UPI-TXN-12345", response.getReferenceNumber());
        assertEquals(PaymentStatus.COMPLETED, response.getStatus());

        // Verify bill status updated to PAID
        assertEquals(BillStatus.PAID, testBill.getStatus());
        verify(billRepository).save(testBill);

        // Verify event publishing
        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(eventPublisher).publishPaymentCompleted(eventCaptor.capture());
        PaymentCompletedEvent publishedEvent = eventCaptor.getValue();
        assertEquals("PaymentCompleted", publishedEvent.getEventType());
        assertEquals(1L, publishedEvent.getPayload().getPaymentId());
        assertEquals(100L, publishedEvent.getPayload().getBillId());
        assertEquals("Jane Smith", publishedEvent.getPayload().getCustomerName());
        assertEquals("jane@example.com", publishedEvent.getPayload().getCustomerEmail());
    }

    @Test
    @DisplayName("Process payment fails when bill does not exist")
    void testProcessPayment_BillNotFound() {
        when(billRepository.findById(999L)).thenReturn(Optional.empty());
        paymentRequest.setBillId(999L);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> paymentService.processPayment(paymentRequest));
        assertTrue(ex.getMessage().contains("Bill not found"));
        verify(paymentRepository, never()).save(any());
        verify(eventPublisher, never()).publishPaymentCompleted(any());
    }

    @Test
    @DisplayName("Process payment fails when bill is already PAID")
    void testProcessPayment_AlreadyPaid() {
        testBill.setStatus(BillStatus.PAID);
        when(billRepository.findById(100L)).thenReturn(Optional.of(testBill));

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> paymentService.processPayment(paymentRequest));
        assertTrue(ex.getMessage().contains("Payment has already been processed"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Process payment fails when bill is in DRAFT status")
    void testProcessPayment_BillDraftStatus() {
        testBill.setStatus(BillStatus.DRAFT);
        when(billRepository.findById(100L)).thenReturn(Optional.of(testBill));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> paymentService.processPayment(paymentRequest));
        assertTrue(ex.getMessage().contains("Payment can only be processed for CONFIRMED bills"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Process payment fails when payment record already exists in repository")
    void testProcessPayment_DuplicatePaymentRecord() {
        when(billRepository.findById(100L)).thenReturn(Optional.of(testBill));
        when(paymentRepository.existsByBillId(100L)).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> paymentService.processPayment(paymentRequest));
        assertTrue(ex.getMessage().contains("A payment record already exists"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Process payment fails when amount paid does not match total amount")
    void testProcessPayment_AmountMismatch() {
        when(billRepository.findById(100L)).thenReturn(Optional.of(testBill));
        when(paymentRepository.existsByBillId(100L)).thenReturn(false);

        paymentRequest.setAmountPaid(new BigDecimal("100.00")); // Mismatch: 100 != 250

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> paymentService.processPayment(paymentRequest));
        assertTrue(ex.getMessage().contains("Amount paid"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get payment by ID successfully")
    void testGetPaymentById_Success() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setBill(testBill);
        payment.setAmountPaid(new BigDecimal("250.00"));
        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaymentDate(LocalDateTime.now());

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        PaymentResponseDTO response = paymentService.getPaymentById(1L);
        assertNotNull(response);
        assertEquals(1L, response.getPaymentId());
        assertEquals(100L, response.getBillId());
        assertEquals(PaymentMethod.CASH, response.getPaymentMethod());
    }

    @Test
    @DisplayName("Get payment by ID throws ResourceNotFoundException when absent")
    void testGetPaymentById_NotFound() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.getPaymentById(999L));
    }

    @Test
    @DisplayName("Get payment by Bill ID successfully")
    void testGetPaymentByBillId_Success() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setBill(testBill);
        payment.setAmountPaid(new BigDecimal("250.00"));
        payment.setPaymentMethod(PaymentMethod.CARD);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaymentDate(LocalDateTime.now());

        when(paymentRepository.findByBillId(100L)).thenReturn(Optional.of(payment));

        PaymentResponseDTO response = paymentService.getPaymentByBillId(100L);
        assertNotNull(response);
        assertEquals(100L, response.getBillId());
        assertEquals(PaymentMethod.CARD, response.getPaymentMethod());
    }

    @Test
    @DisplayName("Get payment by Bill ID throws ResourceNotFoundException when absent")
    void testGetPaymentByBillId_NotFound() {
        when(paymentRepository.findByBillId(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.getPaymentByBillId(999L));
    }
}
