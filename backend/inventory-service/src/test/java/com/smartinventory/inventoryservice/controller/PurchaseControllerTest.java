package com.smartinventory.inventoryservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.inventoryservice.dto.*;
import com.smartinventory.inventoryservice.entity.enums.PurchaseStatus;
import com.smartinventory.inventoryservice.exception.GlobalExceptionHandler;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.service.PurchaseService;
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

@WebMvcTest(PurchaseController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("PurchaseController WebMvc Tests")
class PurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PurchaseService purchaseService;

    private PurchaseResponseDTO samplePurchaseResponse;
    private PurchaseRequestDTO samplePurchaseRequest;

    @BeforeEach
    void setUp() {
        SupplierSummaryDTO supplierSummary = SupplierSummaryDTO.builder()
                .id(1L)
                .name("TechSource Distribution Ltd")
                .build();

        PurchaseItemResponseDTO itemResponse = PurchaseItemResponseDTO.builder()
                .id(1L)
                .productId(1L)
                .productName("Logitech Wireless Mouse M220 Silent")
                .sku("ELEC-LOG-M220")
                .quantity(20)
                .unitCost(new BigDecimal("620.00"))
                .totalCost(new BigDecimal("12400.00"))
                .build();

        samplePurchaseResponse = PurchaseResponseDTO.builder()
                .id(1L)
                .supplier(supplierSummary)
                .purchaseDate(LocalDate.now())
                .totalAmount(new BigDecimal("12400.00"))
                .status(PurchaseStatus.PENDING)
                .notes("Urgent stock replenishment")
                .items(Collections.singletonList(itemResponse))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        samplePurchaseRequest = PurchaseRequestDTO.builder()
                .supplierId(1L)
                .purchaseDate(LocalDate.now())
                .notes("Bulk stationery order")
                .items(Collections.singletonList(
                        PurchaseItemRequestDTO.builder()
                                .productId(1L)
                                .quantity(20)
                                .unitCost(new BigDecimal("620.00"))
                                .build()
                ))
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/purchases: Should return 200 with paginated purchase orders")
    void getPurchases_Success() throws Exception {
        PageResponse<PurchaseResponseDTO> pageResponse = PageResponse.<PurchaseResponseDTO>builder()
                .content(Collections.singletonList(samplePurchaseResponse))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(purchaseService.getPurchases(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/purchases")
                        .param("supplierId", "1")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Purchases retrieved successfully")))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id", is(1)))
                .andExpect(jsonPath("$.data.content[0].supplier.name", is("TechSource Distribution Ltd")))
                .andExpect(jsonPath("$.data.content[0].status", is("PENDING")))
                .andExpect(jsonPath("$.data.totalElements", is(1)));
    }

    @Test
    @DisplayName("GET /api/v1/purchases/{id}: Should return 200 with full purchase order details")
    void getPurchaseById_Success() throws Exception {
        when(purchaseService.getPurchaseById(1L)).thenReturn(samplePurchaseResponse);

        mockMvc.perform(get("/api/v1/purchases/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Purchase order retrieved")))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.supplier.id", is(1)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].productName", is("Logitech Wireless Mouse M220 Silent")))
                .andExpect(jsonPath("$.data.totalAmount", is(12400.00)));
    }

    @Test
    @DisplayName("GET /api/v1/purchases/{id}: Should return 404 when purchase order does not exist")
    void getPurchaseById_NotFound() throws Exception {
        when(purchaseService.getPurchaseById(999L))
                .thenThrow(new ResourceNotFoundException("Purchase order not found with ID: 999"));

        mockMvc.perform(get("/api/v1/purchases/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.error.message", containsString("Purchase order not found with ID: 999")));
    }

    @Test
    @DisplayName("POST /api/v1/purchases: Should return 201 Created when request is valid")
    void createPurchase_Success() throws Exception {
        when(purchaseService.createPurchase(any(PurchaseRequestDTO.class))).thenReturn(samplePurchaseResponse);

        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samplePurchaseRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Purchase order created successfully")))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.items", hasSize(1)));
    }

    @Test
    @DisplayName("POST /api/v1/purchases: Should return 400 when items list is empty")
    void createPurchase_ValidationError_EmptyItems() throws Exception {
        samplePurchaseRequest.setItems(Collections.emptyList());

        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samplePurchaseRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")));

        verify(purchaseService, never()).createPurchase(any());
    }

    @Test
    @DisplayName("PUT /api/v1/purchases/{id}/receive: Should return 200 when purchase is received")
    void receivePurchase_Success() throws Exception {
        PurchaseReceiveResponseDTO receiveResponse = PurchaseReceiveResponseDTO.builder()
                .id(1L)
                .status(PurchaseStatus.RECEIVED)
                .receivedAt(Instant.now())
                .build();

        when(purchaseService.receivePurchase(1L)).thenReturn(receiveResponse);

        mockMvc.perform(put("/api/v1/purchases/1/receive")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Purchase order received and inventory updated successfully")))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.status", is("RECEIVED")))
                .andExpect(jsonPath("$.data.receivedAt", notNullValue()));
    }

    @Test
    @DisplayName("PUT /api/v1/purchases/{id}/receive: Should return 400 when order is in invalid state")
    void receivePurchase_InvalidState() throws Exception {
        when(purchaseService.receivePurchase(1L))
                .thenThrow(new IllegalStateException("Cannot receive purchase order with status: RECEIVED. Only PENDING orders can be received."));

        mockMvc.perform(put("/api/v1/purchases/1/receive")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("INVALID_STATE")))
                .andExpect(jsonPath("$.error.message", containsString("Cannot receive purchase order with status: RECEIVED")));
    }

    @Test
    @DisplayName("PUT /api/v1/purchases/{id}/cancel: Should return 200 when purchase is cancelled")
    void cancelPurchase_Success() throws Exception {
        samplePurchaseResponse.setStatus(PurchaseStatus.CANCELLED);
        when(purchaseService.cancelPurchase(1L)).thenReturn(samplePurchaseResponse);

        mockMvc.perform(put("/api/v1/purchases/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Purchase order cancelled successfully")))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.status", is("CANCELLED")));
    }

    @Test
    @DisplayName("PUT /api/v1/purchases/{id}/cancel: Should return 400 when order cannot be cancelled")
    void cancelPurchase_InvalidState() throws Exception {
        when(purchaseService.cancelPurchase(1L))
                .thenThrow(new IllegalStateException("Cannot cancel purchase order with status: RECEIVED. Only PENDING orders can be cancelled."));

        mockMvc.perform(put("/api/v1/purchases/1/cancel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("INVALID_STATE")))
                .andExpect(jsonPath("$.error.message", containsString("Cannot cancel purchase order with status: RECEIVED")));
    }
}
