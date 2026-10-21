package com.smartinventory.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.payment.dto.PaymentRequestDTO;
import com.smartinventory.payment.dto.PaymentResponseDTO;
import com.smartinventory.payment.entity.enums.PaymentMethod;
import com.smartinventory.payment.entity.enums.PaymentStatus;
import com.smartinventory.payment.exception.DuplicateResourceException;
import com.smartinventory.payment.exception.GlobalExceptionHandler;
import com.smartinventory.payment.exception.ResourceNotFoundException;
import com.smartinventory.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WebMvc slice tests for {@link PaymentController}.
 */
@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/payments - 201 Created on valid payment request")
    void testProcessPayment_Success() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO(100L, new BigDecimal("450.00"), PaymentMethod.CARD, "TXN-999");
        PaymentResponseDTO response = new PaymentResponseDTO(
                1L, 100L, "BILL-001", new BigDecimal("450.00"),
                PaymentMethod.CARD, "TXN-999", PaymentStatus.COMPLETED, LocalDateTime.now()
        );

        when(paymentService.processPayment(any(PaymentRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment processed successfully"))
                .andExpect(jsonPath("$.data.paymentId").value(1))
                .andExpect(jsonPath("$.data.billId").value(100))
                .andExpect(jsonPath("$.data.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/payments - 400 Bad Request on validation failure")
    void testProcessPayment_ValidationError() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO(); // Missing required billId, amount, paymentMethod

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/payments - 409 Conflict when already paid")
    void testProcessPayment_Conflict() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO(100L, new BigDecimal("450.00"), PaymentMethod.UPI, "UPI-1");

        when(paymentService.processPayment(any(PaymentRequestDTO.class)))
                .thenThrow(new DuplicateResourceException("Payment has already been processed for Bill ID: 100"));

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Payment has already been processed for Bill ID: 100"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/{id} - 200 OK")
    void testGetPaymentById_Success() throws Exception {
        PaymentResponseDTO response = new PaymentResponseDTO(
                5L, 105L, "BILL-005", new BigDecimal("120.00"),
                PaymentMethod.CASH, null, PaymentStatus.COMPLETED, LocalDateTime.now()
        );

        when(paymentService.getPaymentById(5L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentId").value(5))
                .andExpect(jsonPath("$.data.paymentMethod").value("CASH"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/{id} - 404 Not Found")
    void testGetPaymentById_NotFound() throws Exception {
        when(paymentService.getPaymentById(99L))
                .thenThrow(new ResourceNotFoundException("Payment not found with ID: 99"));

        mockMvc.perform(get("/api/v1/payments/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Payment not found with ID: 99"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/bill/{billId} - 200 OK")
    void testGetPaymentByBillId_Success() throws Exception {
        PaymentResponseDTO response = new PaymentResponseDTO(
                12L, 50L, "BILL-050", new BigDecimal("75.50"),
                PaymentMethod.BANK_TRANSFER, "REF-BT-1", PaymentStatus.COMPLETED, LocalDateTime.now()
        );

        when(paymentService.getPaymentByBillId(50L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/bill/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentId").value(12))
                .andExpect(jsonPath("$.data.billId").value(50));
    }

    @Test
    @DisplayName("GET /api/v1/payments/bill/{billId} - 404 Not Found")
    void testGetPaymentByBillId_NotFound() throws Exception {
        when(paymentService.getPaymentByBillId(88L))
                .thenThrow(new ResourceNotFoundException("Payment not found for Bill ID: 88"));

        mockMvc.perform(get("/api/v1/payments/bill/88"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
