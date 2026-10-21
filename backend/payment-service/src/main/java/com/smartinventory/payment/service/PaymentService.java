package com.smartinventory.payment.service;

import com.smartinventory.payment.dto.PaymentRequestDTO;
import com.smartinventory.payment.dto.PaymentResponseDTO;

/**
 * Service interface for processing and retrieving payments.
 */
public interface PaymentService {

    /**
     * Processes a payment against an existing, confirmed bill.
     * Validates that the bill is CONFIRMED, no prior payment exists, and amount matches.
     * Transitions bill status to PAID and publishes PaymentCompleted event to RabbitMQ.
     *
     * @param request the payment request data
     * @return the created PaymentResponseDTO
     */
    PaymentResponseDTO processPayment(PaymentRequestDTO request);

    /**
     * Retrieves payment details by payment ID.
     *
     * @param paymentId payment ID
     * @return PaymentResponseDTO
     */
    PaymentResponseDTO getPaymentById(Long paymentId);

    /**
     * Retrieves payment details associated with a specific bill.
     *
     * @param billId bill ID
     * @return PaymentResponseDTO
     */
    PaymentResponseDTO getPaymentByBillId(Long billId);
}
