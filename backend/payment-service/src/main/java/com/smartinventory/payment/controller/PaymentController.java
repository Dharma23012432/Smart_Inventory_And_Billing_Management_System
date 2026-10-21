package com.smartinventory.payment.controller;

import com.smartinventory.payment.dto.ApiResponse;
import com.smartinventory.payment.dto.PaymentRequestDTO;
import com.smartinventory.payment.dto.PaymentResponseDTO;
import com.smartinventory.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Payment operations.
 * Endpoints are prefixed with /api/v1/payments according to specification.
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    /**
     * Constructor injection for PaymentService.
     *
     * @param paymentService the payment service
     */
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Process a payment against a confirmed bill.
     * POST /api/v1/payments
     *
     * @param request the payment request payload
     * @return ApiResponse containing the created PaymentResponseDTO
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> processPayment(@Valid @RequestBody PaymentRequestDTO request) {
        log.info("Received request to process payment for Bill ID: {}", request.getBillId());
        PaymentResponseDTO response = paymentService.processPayment(request);
        return new ResponseEntity<>(
                ApiResponse.success(response, "Payment processed successfully"),
                HttpStatus.CREATED
        );
    }

    /**
     * Get payment details by payment ID.
     * GET /api/v1/payments/{id}
     *
     * @param id payment ID
     * @return ApiResponse containing the PaymentResponseDTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> getPaymentById(@PathVariable("id") Long id) {
        log.info("Received request to get payment by ID: {}", id);
        PaymentResponseDTO response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment retrieved successfully"));
    }

    /**
     * Get payment details by associated bill ID.
     * GET /api/v1/payments/bill/{billId}
     *
     * @param billId bill ID
     * @return ApiResponse containing the PaymentResponseDTO
     */
    @GetMapping("/bill/{billId}")
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> getPaymentByBillId(@PathVariable("billId") Long billId) {
        log.info("Received request to get payment by Bill ID: {}", billId);
        PaymentResponseDTO response = paymentService.getPaymentByBillId(billId);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment retrieved successfully"));
    }
}
