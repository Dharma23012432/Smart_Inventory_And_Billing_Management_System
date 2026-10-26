package com.smartinventory.payment.service.impl;

import com.smartinventory.payment.dto.PaymentRequestDTO;
import com.smartinventory.payment.dto.PaymentResponseDTO;
import com.smartinventory.payment.entity.Bill;
import com.smartinventory.payment.entity.Customer;
import com.smartinventory.payment.entity.Payment;
import com.smartinventory.payment.entity.enums.BillStatus;
import com.smartinventory.payment.entity.enums.PaymentStatus;
import com.smartinventory.payment.event.EventPublisher;
import com.smartinventory.payment.event.PaymentCompletedEvent;
import com.smartinventory.payment.exception.DuplicateResourceException;
import com.smartinventory.payment.exception.ResourceNotFoundException;
import com.smartinventory.payment.repository.BillRepository;
import com.smartinventory.payment.repository.PaymentRepository;
import com.smartinventory.payment.service.PaymentService;
import com.smartinventory.payment.service.mapper.PaymentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Production implementation of {@link PaymentService}.
 * Enforces business rules, transactional state transitions, and event emission.
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final PaymentMapper paymentMapper;
    private final EventPublisher eventPublisher;

    /**
     * Constructor injection for all dependencies.
     *
     * @param paymentRepository the payment repository
     * @param billRepository    the bill repository
     * @param paymentMapper     manual mapper component
     * @param eventPublisher    the messaging event publisher
     */
    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              BillRepository billRepository,
                              PaymentMapper paymentMapper,
                              EventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.billRepository = billRepository;
        this.paymentMapper = paymentMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public PaymentResponseDTO processPayment(PaymentRequestDTO request) {
        log.info("Processing payment for Bill ID: {}, Amount: {}, Method: {}",
                request.getBillId(), request.getAmountPaid(), request.getPaymentMethod());

        // 1. Fetch Bill
        Bill bill = billRepository.findById(request.getBillId())
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with ID: " + request.getBillId()));

        // 2. Validate Bill status
        if (bill.getStatus() == BillStatus.PAID) {
            throw new DuplicateResourceException("Payment has already been processed for Bill ID: " + request.getBillId());
        }
        if (bill.getStatus() != BillStatus.CONFIRMED) {
            throw new IllegalStateException("Payment can only be processed for CONFIRMED bills. Current status: " + bill.getStatus());
        }

        // 3. Double-check unique constraint in repository
        if (paymentRepository.existsByBillId(request.getBillId())) {
            throw new DuplicateResourceException("A payment record already exists for Bill ID: " + request.getBillId());
        }

        // 4. Verify Amount Paid matches Bill Total Amount
        if (bill.getTotalAmount() == null || request.getAmountPaid().compareTo(bill.getTotalAmount()) != 0) {
            throw new IllegalArgumentException(String.format(
                    "Amount paid (%s) must match the bill total amount (%s)",
                    request.getAmountPaid(), bill.getTotalAmount()));
        }

        // 5. Create and save Payment
        Payment payment = new Payment();
        payment.setBill(bill);
        payment.setAmountPaid(request.getAmountPaid());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaymentDate(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment saved successfully with ID: {}", savedPayment.getId());

        // 6. Transition Bill status to PAID
        bill.setStatus(BillStatus.PAID);
        billRepository.save(bill);
        log.info("Bill ID: {} status transitioned to PAID", bill.getId());

        // 7. Publish PaymentCompletedEvent
        publishPaymentCompletedEvent(savedPayment, bill);

        return paymentMapper.toDTO(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentById(Long paymentId) {
        log.debug("Fetching payment by ID: {}", paymentId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));
        return paymentMapper.toDTO(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentByBillId(Long billId) {
        log.debug("Fetching payment by Bill ID: {}", billId);
        Payment payment = paymentRepository.findByBillId(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for Bill ID: " + billId));
        return paymentMapper.toDTO(payment);
    }

    /**
     * Constructs and dispatches the PaymentCompleted event.
     */
    private void publishPaymentCompletedEvent(Payment payment, Bill bill) {
        Customer customer = bill.getCustomer();
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(payment.getId())
                .billId(bill.getId())
                .billNumber(bill.getBillNumber())
                .customerId(customer != null ? customer.getId() : null)
                .customerName(customer != null ? customer.getName() : null)
                .customerEmail(customer != null ? customer.getEmail() : null)
                .amountPaid(payment.getAmountPaid())
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null)
                .paymentDate(payment.getPaymentDate())
                .eventTimestamp(Instant.now())
                .build();

        eventPublisher.publishPaymentCompleted(event);
    }
}
