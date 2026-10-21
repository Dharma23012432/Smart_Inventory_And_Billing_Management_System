package com.smartinventory.inventoryservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.inventoryservice.dto.*;
import com.smartinventory.inventoryservice.entity.enums.BillStatus;
import com.smartinventory.inventoryservice.exception.GlobalExceptionHandler;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.service.BillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BillController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("BillController WebMvc Tests")
class BillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BillService billService;

    private BillResponseDTO sampleBillResponse;
    private BillRequestDTO sampleBillRequest;

    @BeforeEach
    void setUp() {
        CustomerSummaryDTO customerSummary = CustomerSummaryDTO.builder()
                .id(2L)
                .name("Priya Sharma")
                .email("priya.sharma@example.com")
                .build();

        BillItemResponseDTO itemResponse = BillItemResponseDTO.builder()
                .id(201L)
                .productId(1L)
                .productName("Logitech Wireless Mouse M220 Silent")
                .sku("ELEC-LOG-M220")
                .quantity(2)
                .unitPrice(new BigDecimal("899.00"))
                .discountPercent(BigDecimal.ZERO)
                .totalPrice(new BigDecimal("1798.00"))
                .build();

        sampleBillResponse = BillResponseDTO.builder()
                .id(101L)
                .billNumber("INV-2026-0001")
                .customer(customerSummary)
                .billDate(LocalDate.now())
                .subtotal(new BigDecimal("1798.00"))
                .discountAmount(new BigDecimal("100.00"))
                .taxAmount(new BigDecimal("305.64"))
                .totalAmount(new BigDecimal("2003.64"))
                .status(BillStatus.DRAFT)
                .invoiceSent(false)
                .notes("Counter sale")
                .items(Collections.singletonList(itemResponse))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        sampleBillRequest = BillRequestDTO.builder()
                .customerId(2L)
                .discountAmount(new BigDecimal("100.00"))
                .taxPercent(new BigDecimal("18.00"))
                .notes("Counter sale")
                .items(Collections.singletonList(
                        BillItemRequestDTO.builder()
                                .productId(1L)
                                .quantity(2)
                                .discountPercent(BigDecimal.ZERO)
                                .build()
                ))
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/bills: Should return 200 with paginated bills")
    void getBills_Success() throws Exception {
        PageResponse<BillResponseDTO> pageResponse = PageResponse.<BillResponseDTO>builder()
                .content(Collections.singletonList(sampleBillResponse))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(billService.getBills(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/bills")
                        .param("customerId", "2")
                        .param("status", "DRAFT")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Bills retrieved successfully")))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].billNumber", is("INV-2026-0001")))
                .andExpect(jsonPath("$.data.content[0].customer.name", is("Priya Sharma")));
    }

    @Test
    @DisplayName("GET /api/v1/bills/{id}: Should return 200 with full bill details")
    void getBillById_Success() throws Exception {
        when(billService.getBillById(101L)).thenReturn(sampleBillResponse);

        mockMvc.perform(get("/api/v1/bills/101")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Bill retrieved successfully")))
                .andExpect(jsonPath("$.data.id", is(101)))
                .andExpect(jsonPath("$.data.billNumber", is("INV-2026-0001")))
                .andExpect(jsonPath("$.data.totalAmount", is(2003.64)))
                .andExpect(jsonPath("$.data.items", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/v1/bills/{id}: Should return 404 when bill does not exist")
    void getBillById_NotFound() throws Exception {
        when(billService.getBillById(999L))
                .thenThrow(new ResourceNotFoundException("Bill not found with ID: 999"));

        mockMvc.perform(get("/api/v1/bills/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.error.message", containsString("Bill not found with ID: 999")));
    }

    @Test
    @DisplayName("GET /api/v1/bills/number/{billNumber}: Should return 200 with bill details")
    void getBillByBillNumber_Success() throws Exception {
        when(billService.getBillByBillNumber("INV-2026-0001")).thenReturn(sampleBillResponse);

        mockMvc.perform(get("/api/v1/bills/number/INV-2026-0001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Bill retrieved successfully")))
                .andExpect(jsonPath("$.data.billNumber", is("INV-2026-0001")));
    }

    @Test
    @DisplayName("POST /api/v1/bills: Should return 201 Created with created draft bill")
    void createBill_Success() throws Exception {
        when(billService.createBill(any(BillRequestDTO.class))).thenReturn(sampleBillResponse);

        mockMvc.perform(post("/api/v1/bills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleBillRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Bill created successfully in DRAFT status")))
                .andExpect(jsonPath("$.data.id", is(101)))
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andExpect(jsonPath("$.data.billNumber", is("INV-2026-0001")));
    }

    @Test
    @DisplayName("POST /api/v1/bills: Should return 400 when items list is empty")
    void createBill_ValidationError_EmptyItems() throws Exception {
        sampleBillRequest.setItems(Collections.emptyList());

        mockMvc.perform(post("/api/v1/bills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleBillRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")));

        verify(billService, never()).createBill(any());
    }

    @Test
    @DisplayName("PUT /api/v1/bills/{id}/confirm: Should return 200 when bill is confirmed")
    void confirmBill_Success() throws Exception {
        BillConfirmResponseDTO confirmResponse = BillConfirmResponseDTO.builder()
                .id(101L)
                .billNumber("INV-2026-0001")
                .status(BillStatus.CONFIRMED)
                .totalAmount(new BigDecimal("2003.64"))
                .build();

        when(billService.confirmBill(101L)).thenReturn(confirmResponse);

        mockMvc.perform(put("/api/v1/bills/101/confirm")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Bill confirmed and inventory deducted successfully")))
                .andExpect(jsonPath("$.data.id", is(101)))
                .andExpect(jsonPath("$.data.billNumber", is("INV-2026-0001")))
                .andExpect(jsonPath("$.data.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.data.totalAmount", is(2003.64)));
    }

    @Test
    @DisplayName("PUT /api/v1/bills/{id}/confirm: Should return 400 when stock is insufficient")
    void confirmBill_InsufficientStock() throws Exception {
        when(billService.confirmBill(101L))
                .thenThrow(new IllegalStateException("Insufficient stock for product 'Logitech Wireless Mouse M220 Silent' (SKU: ELEC-LOG-M220). Available: 1, Requested: 2"));

        mockMvc.perform(put("/api/v1/bills/101/confirm")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("INVALID_STATE")))
                .andExpect(jsonPath("$.error.message", containsString("Insufficient stock for product")));
    }

    @Test
    @DisplayName("PUT /api/v1/bills/{id}/cancel: Should return 200 when bill is cancelled")
    void cancelBill_Success() throws Exception {
        sampleBillResponse.setStatus(BillStatus.CANCELLED);
        when(billService.cancelBill(101L)).thenReturn(sampleBillResponse);

        mockMvc.perform(put("/api/v1/bills/101/cancel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Bill cancelled successfully")))
                .andExpect(jsonPath("$.data.id", is(101)))
                .andExpect(jsonPath("$.data.status", is("CANCELLED")));
    }

    @Test
    @DisplayName("PUT /api/v1/bills/{id}/cancel: Should return 400 when bill is already PAID")
    void cancelBill_InvalidState() throws Exception {
        when(billService.cancelBill(101L))
                .thenThrow(new IllegalStateException("Bills in PAID status cannot be cancelled in V1. Please initiate a formal refund process."));

        mockMvc.perform(put("/api/v1/bills/101/cancel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("INVALID_STATE")))
                .andExpect(jsonPath("$.error.message", containsString("Bills in PAID status cannot be cancelled in V1")));
    }
}
