package com.smartinventory.inventoryservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.inventoryservice.dto.PageResponse;
import com.smartinventory.inventoryservice.dto.SupplierRequestDTO;
import com.smartinventory.inventoryservice.dto.SupplierResponseDTO;
import com.smartinventory.inventoryservice.exception.DuplicateResourceException;
import com.smartinventory.inventoryservice.exception.GlobalExceptionHandler;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.service.SupplierService;
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

@WebMvcTest(SupplierController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("SupplierController WebMvc Tests")
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SupplierService supplierService;

    private SupplierResponseDTO sampleResponse;
    private SupplierRequestDTO sampleRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = SupplierResponseDTO.builder()
                .id(1L)
                .name("TechSource Distribution Ltd")
                .contactPerson("Rajesh Sharma")
                .email("orders@techsource.com")
                .phone("+91 98765 43210")
                .address("100 Industrial Area, Phase 2, Bangalore")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        sampleRequest = SupplierRequestDTO.builder()
                .name("TechSource Distribution Ltd")
                .contactPerson("Rajesh Sharma")
                .email("orders@techsource.com")
                .phone("+91 98765 43210")
                .address("100 Industrial Area, Phase 2, Bangalore")
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/suppliers should return 200 OK and paginated response")
    void getSuppliers_shouldReturn200() throws Exception {
        PageResponse<SupplierResponseDTO> pageResponse = PageResponse.<SupplierResponseDTO>builder()
                .content(Collections.singletonList(sampleResponse))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(supplierService.getSuppliers(any(), any(), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/suppliers")
                        .param("search", "tech")
                        .param("status", "ACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Suppliers retrieved successfully")))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].email", is("orders@techsource.com")));

        verify(supplierService, times(1)).getSuppliers(eq("tech"), eq("ACTIVE"), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/v1/suppliers/active should return 200 OK and list of active suppliers")
    void getAllActiveSuppliers_shouldReturn200() throws Exception {
        when(supplierService.getAllActiveSuppliers()).thenReturn(Collections.singletonList(sampleResponse));

        mockMvc.perform(get("/api/v1/suppliers/active")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(1)));

        verify(supplierService, times(1)).getAllActiveSuppliers();
    }

    @Test
    @DisplayName("GET /api/v1/suppliers/{id} should return 200 OK when found")
    void getSupplierById_whenExists_shouldReturn200() throws Exception {
        when(supplierService.getSupplierById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/suppliers/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.name", is("TechSource Distribution Ltd")));

        verify(supplierService, times(1)).getSupplierById(1L);
    }

    @Test
    @DisplayName("GET /api/v1/suppliers/{id} should return 404 when not found")
    void getSupplierById_whenNotFound_shouldReturn404() throws Exception {
        when(supplierService.getSupplierById(99L))
                .thenThrow(new ResourceNotFoundException("Supplier", "id", 99L));

        mockMvc.perform(get("/api/v1/suppliers/99")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.error.message", containsString("Supplier with id '99' was not found")));
    }

    @Test
    @DisplayName("POST /api/v1/suppliers should return 201 Created on valid payload")
    void createSupplier_validPayload_shouldReturn201() throws Exception {
        when(supplierService.createSupplier(any(SupplierRequestDTO.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Supplier created successfully")))
                .andExpect(jsonPath("$.data.email", is("orders@techsource.com")));

        verify(supplierService, times(1)).createSupplier(any(SupplierRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/suppliers should return 400 Bad Request on invalid payload")
    void createSupplier_invalidPayload_shouldReturn400() throws Exception {
        SupplierRequestDTO invalidRequest = SupplierRequestDTO.builder()
                .name("") // blank
                .email("not-an-email") // invalid email
                .build();

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.error.details", not(empty())));

        verify(supplierService, never()).createSupplier(any());
    }

    @Test
    @DisplayName("POST /api/v1/suppliers should return 409 Conflict when email already exists")
    void createSupplier_duplicateEmail_shouldReturn409() throws Exception {
        when(supplierService.createSupplier(any(SupplierRequestDTO.class)))
                .thenThrow(new DuplicateResourceException("Supplier", "email", "orders@techsource.com"));

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("DUPLICATE_RESOURCE")))
                .andExpect(jsonPath("$.error.message", containsString("Supplier with email 'orders@techsource.com' already exists")));
    }

    @Test
    @DisplayName("PUT /api/v1/suppliers/{id} should return 200 OK on valid update")
    void updateSupplier_validPayload_shouldReturn200() throws Exception {
        when(supplierService.updateSupplier(eq(1L), any(SupplierRequestDTO.class))).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/v1/suppliers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Supplier updated successfully")));

        verify(supplierService, times(1)).updateSupplier(eq(1L), any(SupplierRequestDTO.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/suppliers/{id} should return 200 OK")
    void deleteSupplier_shouldReturn200() throws Exception {
        doNothing().when(supplierService).deleteSupplier(1L);

        mockMvc.perform(delete("/api/v1/suppliers/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Supplier deleted successfully")));

        verify(supplierService, times(1)).deleteSupplier(1L);
    }

    @Test
    @DisplayName("PATCH /api/v1/suppliers/{id}/status should return 200 OK on status transition")
    void updateSupplierStatus_shouldReturn200() throws Exception {
        sampleResponse.setStatus("INACTIVE");
        when(supplierService.updateSupplierStatus(1L, "INACTIVE")).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/v1/suppliers/1/status")
                        .param("status", "INACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("INACTIVE")));

        verify(supplierService, times(1)).updateSupplierStatus(1L, "INACTIVE");
    }
}
