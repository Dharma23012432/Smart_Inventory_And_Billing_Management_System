package com.smartinventory.inventoryservice.controller;

import com.smartinventory.inventoryservice.dto.ApiResponse;
import com.smartinventory.inventoryservice.dto.PageResponse;
import com.smartinventory.inventoryservice.dto.SupplierRequestDTO;
import com.smartinventory.inventoryservice.dto.SupplierResponseDTO;
import com.smartinventory.inventoryservice.service.SupplierService;
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
 * REST controller exposing Supplier management endpoints.
 * Strictly adheres to Section 3.4 of API_SPECIFICATION.md with consistent /api/v1 prefix.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    /**
     * Constructor injection for SupplierService.
     *
     * @param supplierService Supplier business operations service
     */
    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    /**
     * GET /api/v1/suppliers
     * Retrieves a paginated and filtered list of suppliers.
     *
     * @param search    Optional search text across name, contact person, and email
     * @param status    Optional status filter (e.g. ACTIVE, INACTIVE, ALL - defaults to ACTIVE)
     * @param page      Page index (0-based, default: 0)
     * @param size      Page size (default: 10)
     * @param sortBy    Sort field (default: "name")
     * @param direction Sort direction (ASC or DESC, default: "ASC")
     * @return Paginated suppliers wrapped in standardized ApiResponse envelope
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SupplierResponseDTO>>> getSuppliers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "ACTIVE") String status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String direction) {

        log.info("REST request to get suppliers: search='{}', status='{}', page={}, size={}",
                search, status, page, size);

        Sort.Direction sortDirection = "DESC".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        PageResponse<SupplierResponseDTO> response = supplierService.getSuppliers(search, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Suppliers retrieved successfully", response));
    }

    /**
     * GET /api/v1/suppliers/active
     * Retrieves all active suppliers without pagination for select/dropdown components.
     *
     * @return List of active SupplierResponseDTOs wrapped in ApiResponse
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<SupplierResponseDTO>>> getAllActiveSuppliers() {
        log.info("REST request to get all active suppliers");
        List<SupplierResponseDTO> activeSuppliers = supplierService.getAllActiveSuppliers();
        return ResponseEntity.ok(ApiResponse.success("Active suppliers retrieved successfully", activeSuppliers));
    }

    /**
     * GET /api/v1/suppliers/{id}
     * Retrieves single supplier details by primary key.
     *
     * @param id Supplier primary key
     * @return Single SupplierResponseDTO wrapped in standardized ApiResponse envelope
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponseDTO>> getSupplierById(@PathVariable Long id) {
        log.info("REST request to get supplier with ID: {}", id);
        SupplierResponseDTO supplier = supplierService.getSupplierById(id);
        return ResponseEntity.ok(ApiResponse.success("Supplier retrieved successfully", supplier));
    }

    /**
     * POST /api/v1/suppliers
     * Registers a new supplier vendor.
     *
     * @param requestDTO Validated inbound supplier creation payload
     * @return Created SupplierResponseDTO wrapped in ApiResponse with HTTP 201 Created
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SupplierResponseDTO>> createSupplier(
            @Valid @RequestBody SupplierRequestDTO requestDTO) {

        log.info("REST request to create supplier with email: '{}', name: '{}'",
                requestDTO.getEmail(), requestDTO.getName());
        SupplierResponseDTO createdSupplier = supplierService.createSupplier(requestDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Supplier created successfully", createdSupplier));
    }

    /**
     * PUT /api/v1/suppliers/{id}
     * Updates an existing supplier's details.
     *
     * @param id         Supplier primary key
     * @param requestDTO Validated inbound supplier update payload
     * @return Updated SupplierResponseDTO wrapped in standardized ApiResponse envelope
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponseDTO>> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequestDTO requestDTO) {

        log.info("REST request to update supplier with ID: {}", id);
        SupplierResponseDTO updatedSupplier = supplierService.updateSupplier(id, requestDTO);

        return ResponseEntity.ok(ApiResponse.success("Supplier updated successfully", updatedSupplier));
    }

    /**
     * DELETE /api/v1/suppliers/{id}
     * Deletes a supplier if no transaction history exists.
     *
     * @param id Supplier primary key
     * @return Empty data response wrapped in standardized ApiResponse envelope
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(@PathVariable Long id) {
        log.info("REST request to delete supplier with ID: {}", id);
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(ApiResponse.success("Supplier deleted successfully", null));
    }

    /**
     * PATCH /api/v1/suppliers/{id}/status
     * Explicitly transitions supplier lifecycle status (ACTIVE / INACTIVE).
     *
     * @param id     Supplier primary key
     * @param status Target status ('ACTIVE' or 'INACTIVE')
     * @return Updated SupplierResponseDTO wrapped in standardized ApiResponse envelope
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SupplierResponseDTO>> updateSupplierStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        log.info("REST request to update status for supplier ID: {} to '{}'", id, status);
        SupplierResponseDTO updatedSupplier = supplierService.updateSupplierStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Supplier status updated successfully", updatedSupplier));
    }
}
