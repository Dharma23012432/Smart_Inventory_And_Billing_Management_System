package com.smartinventory.inventoryservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.inventoryservice.dto.*;
import com.smartinventory.inventoryservice.exception.GlobalExceptionHandler;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.service.InventoryService;
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

import java.time.Instant;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("InventoryController WebMvc Tests")
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    private InventoryResponseDTO sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = InventoryResponseDTO.builder()
                .inventoryId(100L)
                .productId(10L)
                .productName("Wireless Optical Mouse")
                .sku("ELEC-MOU-001")
                .quantityOnHand(8)
                .reorderLevel(10)
                .maxStockLevel(100)
                .isLowStock(true)
                .version(0L)
                .lastUpdatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/inventory should return 200 OK and paginated response")
    void getInventoryList_shouldReturn200() throws Exception {
        PageResponse<InventoryResponseDTO> pageResponse = PageResponse.<InventoryResponseDTO>builder()
                .content(Collections.singletonList(sampleResponse))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(inventoryService.getInventoryList(eq(false), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory")
                        .param("lowStockOnly", "false")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Inventory retrieved successfully")))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].inventoryId", is(100)))
                .andExpect(jsonPath("$.data.content[0].quantityOnHand", is(8)));

        verify(inventoryService, times(1)).getInventoryList(eq(false), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/{id} should return 200 OK when found")
    void getInventoryById_whenExists_shouldReturn200() throws Exception {
        when(inventoryService.getInventoryById(100L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/inventory/100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.inventoryId", is(100)))
                .andExpect(jsonPath("$.data.sku", is("ELEC-MOU-001")));

        verify(inventoryService, times(1)).getInventoryById(100L);
    }

    @Test
    @DisplayName("GET /api/v1/inventory/{id} should return 404 when not found")
    void getInventoryById_whenNotFound_shouldReturn404() throws Exception {
        when(inventoryService.getInventoryById(999L))
                .thenThrow(new ResourceNotFoundException("Inventory", "id", 999L));

        mockMvc.perform(get("/api/v1/inventory/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.error.message", containsString("Inventory with id '999' was not found")));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/product/{productId} should return 200 OK when found")
    void getInventoryByProductId_whenExists_shouldReturn200() throws Exception {
        when(inventoryService.getInventoryByProductId(10L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/inventory/product/10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.productId", is(10)))
                .andExpect(jsonPath("$.data.quantityOnHand", is(8)));

        verify(inventoryService, times(1)).getInventoryByProductId(10L);
    }

    @Test
    @DisplayName("GET /api/v1/inventory/product/{productId} should return 404 when not found")
    void getInventoryByProductId_whenNotFound_shouldReturn404() throws Exception {
        when(inventoryService.getInventoryByProductId(99L))
                .thenThrow(new ResourceNotFoundException("Inventory", "productId", 99L));

        mockMvc.perform(get("/api/v1/inventory/product/99")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.error.message", containsString("Inventory with productId '99' was not found")));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/low-stock should return 200 OK")
    void getLowStockInventory_shouldReturn200() throws Exception {
        PageResponse<InventoryResponseDTO> pageResponse = PageResponse.<InventoryResponseDTO>builder()
                .content(Collections.singletonList(sampleResponse))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(inventoryService.getLowStockInventory(any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inventory/low-stock")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].isLowStock", is(true)));

        verify(inventoryService, times(1)).getLowStockInventory(any(Pageable.class));
    }

    @Test
    @DisplayName("POST /api/v1/inventory/product/{productId}/adjust should return 200 OK on valid adjustment")
    void adjustStock_validPayload_shouldReturn200() throws Exception {
        sampleResponse.setQuantityOnHand(50);
        sampleResponse.setIsLowStock(false);

        when(inventoryService.adjustStock(eq(10L), any(InventoryAdjustmentRequestDTO.class)))
                .thenReturn(sampleResponse);

        InventoryAdjustmentRequestDTO requestDTO = InventoryAdjustmentRequestDTO.builder()
                .adjustedQuantity(50)
                .reason("Reconciliation count")
                .build();

        mockMvc.perform(post("/api/v1/inventory/product/10/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Stock adjusted successfully")))
                .andExpect(jsonPath("$.data.quantityOnHand", is(50)));

        verify(inventoryService, times(1)).adjustStock(eq(10L), any(InventoryAdjustmentRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/inventory/product/{productId}/adjust should return 400 Bad Request on negative quantity or blank reason")
    void adjustStock_invalidPayload_shouldReturn400() throws Exception {
        InventoryAdjustmentRequestDTO invalidRequest = InventoryAdjustmentRequestDTO.builder()
                .adjustedQuantity(-5)
                .reason("   ") // blank
                .build();

        mockMvc.perform(post("/api/v1/inventory/product/10/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.error.details", not(empty())));

        verify(inventoryService, never()).adjustStock(any(), any());
    }

    @Test
    @DisplayName("PUT /api/v1/inventory/product/{productId}/settings should return 200 OK on valid thresholds")
    void updateReorderSettings_validPayload_shouldReturn200() throws Exception {
        sampleResponse.setReorderLevel(15);
        sampleResponse.setMaxStockLevel(120);

        when(inventoryService.updateReorderSettings(eq(10L), any(InventorySettingsRequestDTO.class)))
                .thenReturn(sampleResponse);

        InventorySettingsRequestDTO requestDTO = InventorySettingsRequestDTO.builder()
                .reorderLevel(15)
                .maxStockLevel(120)
                .build();

        mockMvc.perform(put("/api/v1/inventory/product/10/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Inventory settings updated successfully")))
                .andExpect(jsonPath("$.data.reorderLevel", is(15)))
                .andExpect(jsonPath("$.data.maxStockLevel", is(120)));

        verify(inventoryService, times(1)).updateReorderSettings(eq(10L), any(InventorySettingsRequestDTO.class));
    }

    @Test
    @DisplayName("PATCH /api/v1/inventory/product/{productId}/reorder-level should return 200 OK")
    void updateReorderLevel_shouldReturn200() throws Exception {
        sampleResponse.setReorderLevel(25);
        when(inventoryService.updateReorderLevel(10L, 25)).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/v1/inventory/product/10/reorder-level")
                        .param("reorderLevel", "25")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.reorderLevel", is(25)));

        verify(inventoryService, times(1)).updateReorderLevel(10L, 25);
    }

    @Test
    @DisplayName("PATCH /api/v1/inventory/product/{productId}/max-stock-level should return 200 OK")
    void updateMaxStockLevel_shouldReturn200() throws Exception {
        sampleResponse.setMaxStockLevel(250);
        when(inventoryService.updateMaxStockLevel(10L, 250)).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/v1/inventory/product/10/max-stock-level")
                        .param("maxStockLevel", "250")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.maxStockLevel", is(250)));

        verify(inventoryService, times(1)).updateMaxStockLevel(10L, 250);
    }
}
