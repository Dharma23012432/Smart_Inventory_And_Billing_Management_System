package com.smartinventory.inventoryservice.controller;

import com.smartinventory.inventoryservice.dto.ApiResponse;
import com.smartinventory.inventoryservice.dto.CustomerRequestDTO;
import com.smartinventory.inventoryservice.dto.CustomerResponseDTO;
import com.smartinventory.inventoryservice.dto.PageResponse;
import com.smartinventory.inventoryservice.service.CustomerService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing Customer management endpoints.
 * Strictly adheres to Section 3.5 of API_SPECIFICATION.md with consistent /api/v1 prefix.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Constructor injection for CustomerService.
     *
     * @param customerService Customer business operations service
     */
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * GET /api/v1/customers
     * Retrieves a paginated and filtered list of customers.
     *
     * @param search    Optional search text across name, phone, and email
     * @param status    Optional status filter (e.g. ACTIVE, INACTIVE, ALL - defaults to ACTIVE)
     * @param page      Page index (0-based, default: 0)
     * @param size      Page size (default: 10)
     * @param sortBy    Sort field (default: "name")
     * @param direction Sort direction (ASC or DESC, default: "ASC")
     * @return Paginated customers wrapped in standardized ApiResponse envelope
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponseDTO>>> getCustomers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "ACTIVE") String status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String direction) {

        log.info("REST request to get customers: search='{}', status='{}', page={}, size={}",
                search, status, page, size);

        Sort.Direction sortDirection = "DESC".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        PageResponse<CustomerResponseDTO> response = customerService.getCustomers(search, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved successfully", response));
    }

    /**
     * GET /api/v1/customers/active
     * Retrieves all active customers without pagination for billing select/search dropdowns.
     *
     * @return List of active CustomerResponseDTOs wrapped in ApiResponse
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<CustomerResponseDTO>>> getAllActiveCustomers() {
        log.info("REST request to get all active customers");
        List<CustomerResponseDTO> activeCustomers = customerService.getAllActiveCustomers();
        return ResponseEntity.ok(ApiResponse.success("Active customers retrieved successfully", activeCustomers));
    }

    /**
     * GET /api/v1/customers/{id}
     * Retrieves single customer details by primary key.
     *
     * @param id Customer primary key
     * @return Single CustomerResponseDTO wrapped in standardized ApiResponse envelope
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> getCustomerById(@PathVariable Long id) {
        log.info("REST request to get customer with ID: {}", id);
        CustomerResponseDTO customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success("Customer retrieved successfully", customer));
    }

    /**
     * POST /api/v1/customers
     * Registers a new customer profile.
     *
     * @param requestDTO Validated inbound customer creation payload
     * @return Created CustomerResponseDTO wrapped in ApiResponse with HTTP 201 Created
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> createCustomer(
            @Valid @RequestBody CustomerRequestDTO requestDTO) {

        log.info("REST request to create customer with name: '{}', phone: '{}'",
                requestDTO.getName(), requestDTO.getPhone());
        CustomerResponseDTO createdCustomer = customerService.createCustomer(requestDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created successfully", createdCustomer));
    }

    /**
     * PUT /api/v1/customers/{id}
     * Updates an existing customer's contact details.
     *
     * @param id         Customer primary key
     * @param requestDTO Validated inbound customer update payload
     * @return Updated CustomerResponseDTO wrapped in standardized ApiResponse envelope
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequestDTO requestDTO) {

        log.info("REST request to update customer with ID: {}", id);
        CustomerResponseDTO updatedCustomer = customerService.updateCustomer(id, requestDTO);

        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", updatedCustomer));
    }

    /**
     * DELETE /api/v1/customers/{id}
     * Deletes a customer if no billing history exists.
     *
     * @param id Customer primary key
     * @return Empty data response wrapped in standardized ApiResponse envelope
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long id) {
        log.info("REST request to delete customer with ID: {}", id);
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully", null));
    }

    /**
     * PATCH /api/v1/customers/{id}/status
     * Explicitly transitions customer lifecycle status (ACTIVE / INACTIVE).
     *
     * @param id     Customer primary key
     * @param status Target status ('ACTIVE' or 'INACTIVE')
     * @return Updated CustomerResponseDTO wrapped in standardized ApiResponse envelope
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> updateCustomerStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        log.info("REST request to update status for customer ID: {} to '{}'", id, status);
        CustomerResponseDTO updatedCustomer = customerService.updateCustomerStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Customer status updated successfully", updatedCustomer));
    }
}
