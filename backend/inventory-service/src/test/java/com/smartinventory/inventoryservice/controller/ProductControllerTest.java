package com.smartinventory.inventoryservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.inventoryservice.dto.*;
import com.smartinventory.inventoryservice.exception.DuplicateResourceException;
import com.smartinventory.inventoryservice.exception.GlobalExceptionHandler;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.service.ProductService;
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
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("ProductController WebMvc Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductResponseDTO sampleResponse;
    private ProductRequestDTO sampleRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = ProductResponseDTO.builder()
                .id(1L)
                .name("Logitech Wireless Mouse M220")
                .sku("ELEC-LOG-M220")
                .description("Silent wireless mouse")
                .unitPrice(new BigDecimal("899.00"))
                .purchasePrice(new BigDecimal("620.00"))
                .unitOfMeasure("PCS")
                .status("ACTIVE")
                .category(CategorySummaryDTO.builder().id(1L).name("Electronics").build())
                .supplier(SupplierSummaryDTO.builder().id(1L).name("TechSource Ltd").build())
                .inventory(InventorySummaryDTO.builder()
                        .quantityOnHand(35)
                        .reorderLevel(10)
                        .maxStockLevel(100)
                        .isLowStock(false)
                        .build())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        sampleRequest = ProductRequestDTO.builder()
                .categoryId(1L)
                .supplierId(1L)
                .name("Logitech Wireless Mouse M220")
                .sku("ELEC-LOG-M220")
                .description("Silent wireless mouse")
                .unitPrice(new BigDecimal("899.00"))
                .purchasePrice(new BigDecimal("620.00"))
                .unitOfMeasure("PCS")
                .status("ACTIVE")
                .initialStock(35)
                .reorderLevel(10)
                .maxStockLevel(100)
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/products should return 200 OK and paginated list")
    void getProducts_shouldReturn200() throws Exception {
        PageResponse<ProductResponseDTO> pageResponse = PageResponse.<ProductResponseDTO>builder()
                .content(Collections.singletonList(sampleResponse))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(productService.getProducts(any(), any(), any(), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/products")
                        .param("search", "mouse")
                        .param("categoryId", "1")
                        .param("status", "ACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Products retrieved")))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].sku", is("ELEC-LOG-M220")))
                .andExpect(jsonPath("$.data.content[0].inventory.quantityOnHand", is(35)));

        verify(productService, times(1)).getProducts(eq("mouse"), eq(1L), eq("ACTIVE"), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} should return 200 OK when found")
    void getProductById_whenExists_shouldReturn200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.sku", is("ELEC-LOG-M220")));

        verify(productService, times(1)).getProductById(1L);
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} should return 404 when not found")
    void getProductById_whenNotFound_shouldReturn404() throws Exception {
        when(productService.getProductById(99L))
                .thenThrow(new ResourceNotFoundException("Product", "id", 99L));

        mockMvc.perform(get("/api/v1/products/99")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.error.message", containsString("Product with id '99' was not found")));
    }

    @Test
    @DisplayName("GET /api/v1/products/sku/{sku} should return 200 OK when found")
    void getProductBySku_whenExists_shouldReturn200() throws Exception {
        when(productService.getProductBySku("ELEC-LOG-M220")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/products/sku/ELEC-LOG-M220")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.sku", is("ELEC-LOG-M220")));

        verify(productService, times(1)).getProductBySku("ELEC-LOG-M220");
    }

    @Test
    @DisplayName("GET /api/v1/products/sku/{sku} should return 404 when not found")
    void getProductBySku_whenNotFound_shouldReturn404() throws Exception {
        when(productService.getProductBySku("UNKNOWN"))
                .thenThrow(new ResourceNotFoundException("Product", "sku", "UNKNOWN"));

        mockMvc.perform(get("/api/v1/products/sku/UNKNOWN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("POST /api/v1/products should return 201 Created on valid payload")
    void createProduct_validPayload_shouldReturn201() throws Exception {
        when(productService.createProduct(any(ProductRequestDTO.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Product created successfully")))
                .andExpect(jsonPath("$.data.sku", is("ELEC-LOG-M220")));

        verify(productService, times(1)).createProduct(any(ProductRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/products should return 400 Bad Request on invalid payload")
    void createProduct_invalidPayload_shouldReturn400() throws Exception {
        ProductRequestDTO invalidRequest = ProductRequestDTO.builder()
                .name("") // blank
                .sku("")  // blank
                .unitPrice(new BigDecimal("-10.00")) // negative
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.error.details", not(empty())));

        verify(productService, never()).createProduct(any());
    }

    @Test
    @DisplayName("POST /api/v1/products should return 409 Conflict when SKU already exists")
    void createProduct_duplicateSku_shouldReturn409() throws Exception {
        when(productService.createProduct(any(ProductRequestDTO.class)))
                .thenThrow(new DuplicateResourceException("Product", "sku", "ELEC-LOG-M220"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("DUPLICATE_RESOURCE")))
                .andExpect(jsonPath("$.error.message", containsString("Product with sku 'ELEC-LOG-M220' already exists")));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id} should return 200 OK on valid update")
    void updateProduct_validPayload_shouldReturn200() throws Exception {
        when(productService.updateProduct(eq(1L), any(ProductRequestDTO.class))).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Product updated successfully")))
                .andExpect(jsonPath("$.data.id", is(1)));

        verify(productService, times(1)).updateProduct(eq(1L), any(ProductRequestDTO.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id} should return 200 OK")
    void deleteProduct_shouldReturn200() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Product deleted successfully")));

        verify(productService, times(1)).deleteProduct(1L);
    }

    @Test
    @DisplayName("PATCH /api/v1/products/{id}/status should return 200 OK on status transition")
    void updateProductStatus_shouldReturn200() throws Exception {
        sampleResponse.setStatus("INACTIVE");
        when(productService.updateProductStatus(1L, "INACTIVE")).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/v1/products/1/status")
                        .param("status", "INACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("INACTIVE")));

        verify(productService, times(1)).updateProductStatus(1L, "INACTIVE");
    }
}
