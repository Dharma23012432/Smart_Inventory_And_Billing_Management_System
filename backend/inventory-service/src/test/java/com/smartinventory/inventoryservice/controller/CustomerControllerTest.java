package com.smartinventory.inventoryservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.inventoryservice.dto.CustomerRequestDTO;
import com.smartinventory.inventoryservice.dto.CustomerResponseDTO;
import com.smartinventory.inventoryservice.dto.PageResponse;
import com.smartinventory.inventoryservice.exception.GlobalExceptionHandler;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.service.CustomerService;
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

@WebMvcTest(CustomerController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("CustomerController WebMvc Tests")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    private CustomerResponseDTO sampleResponse;
    private CustomerRequestDTO sampleRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = CustomerResponseDTO.builder()
                .id(1L)
                .name("Kavita Rao")
                .email("kavita.rao@example.com")
                .phone("+91 98765 12345")
                .address("12 Residency Road, Bangalore, KA")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        sampleRequest = CustomerRequestDTO.builder()
                .name("Kavita Rao")
                .email("kavita.rao@example.com")
                .phone("+91 98765 12345")
                .address("12 Residency Road, Bangalore, KA")
                .status("ACTIVE")
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/customers should return 200 OK and paginated response")
    void getCustomers_shouldReturn200() throws Exception {
        PageResponse<CustomerResponseDTO> pageResponse = PageResponse.<CustomerResponseDTO>builder()
                .content(Collections.singletonList(sampleResponse))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(customerService.getCustomers(any(), any(), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/customers")
                        .param("search", "kavita")
                        .param("status", "ACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Customers retrieved successfully")))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name", is("Kavita Rao")));

        verify(customerService, times(1)).getCustomers(eq("kavita"), eq("ACTIVE"), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/v1/customers/active should return 200 OK and list of active customers")
    void getAllActiveCustomers_shouldReturn200() throws Exception {
        when(customerService.getAllActiveCustomers()).thenReturn(Collections.singletonList(sampleResponse));

        mockMvc.perform(get("/api/v1/customers/active")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(1)));

        verify(customerService, times(1)).getAllActiveCustomers();
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id} should return 200 OK when found")
    void getCustomerById_whenExists_shouldReturn200() throws Exception {
        when(customerService.getCustomerById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/customers/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.name", is("Kavita Rao")));

        verify(customerService, times(1)).getCustomerById(1L);
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id} should return 404 when not found")
    void getCustomerById_whenNotFound_shouldReturn404() throws Exception {
        when(customerService.getCustomerById(99L))
                .thenThrow(new ResourceNotFoundException("Customer", "id", 99L));

        mockMvc.perform(get("/api/v1/customers/99")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.error.message", containsString("Customer with id '99' was not found")));
    }

    @Test
    @DisplayName("POST /api/v1/customers should return 201 Created on valid payload")
    void createCustomer_validPayload_shouldReturn201() throws Exception {
        when(customerService.createCustomer(any(CustomerRequestDTO.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Customer created successfully")))
                .andExpect(jsonPath("$.data.name", is("Kavita Rao")));

        verify(customerService, times(1)).createCustomer(any(CustomerRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/v1/customers should return 400 Bad Request on blank name")
    void createCustomer_invalidPayload_shouldReturn400() throws Exception {
        CustomerRequestDTO invalidRequest = CustomerRequestDTO.builder()
                .name("") // blank
                .email("not-an-email") // invalid email
                .build();

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.error.details", not(empty())));

        verify(customerService, never()).createCustomer(any());
    }

    @Test
    @DisplayName("PUT /api/v1/customers/{id} should return 200 OK on valid update")
    void updateCustomer_validPayload_shouldReturn200() throws Exception {
        when(customerService.updateCustomer(eq(1L), any(CustomerRequestDTO.class))).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/v1/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Customer updated successfully")));

        verify(customerService, times(1)).updateCustomer(eq(1L), any(CustomerRequestDTO.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/customers/{id} should return 200 OK")
    void deleteCustomer_shouldReturn200() throws Exception {
        doNothing().when(customerService).deleteCustomer(1L);

        mockMvc.perform(delete("/api/v1/customers/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Customer deleted successfully")));

        verify(customerService, times(1)).deleteCustomer(1L);
    }

    @Test
    @DisplayName("PATCH /api/v1/customers/{id}/status should return 200 OK on status transition")
    void updateCustomerStatus_shouldReturn200() throws Exception {
        sampleResponse.setStatus("INACTIVE");
        when(customerService.updateCustomerStatus(1L, "INACTIVE")).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/v1/customers/1/status")
                        .param("status", "INACTIVE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("INACTIVE")));

        verify(customerService, times(1)).updateCustomerStatus(1L, "INACTIVE");
    }
}
