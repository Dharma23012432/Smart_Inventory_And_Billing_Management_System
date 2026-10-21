package com.smartinventory.inventoryservice.service;

import com.smartinventory.inventoryservice.dto.PageResponse;
import com.smartinventory.inventoryservice.dto.SupplierRequestDTO;
import com.smartinventory.inventoryservice.dto.SupplierResponseDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface defining business operations for Supplier management.
 * Strictly adheres to Section 3.4 of API_SPECIFICATION.md.
 */
public interface SupplierService {

    /**
     * Retrieves a paginated list of suppliers matching optional search query and status filter.
     *
     * @param search   Optional search text across name, contact person, or email
     * @param status   Optional status filter (e.g. ACTIVE, INACTIVE, ALL)
     * @param pageable Pagination and sorting parameters
     * @return Standardized PageResponse of SupplierResponseDTOs
     */
    PageResponse<SupplierResponseDTO> getSuppliers(String search, String status, Pageable pageable);

    /**
     * Retrieves all active suppliers sorted by name for UI dropdown selections.
     *
     * @return List of active SupplierResponseDTOs
     */
    List<SupplierResponseDTO> getAllActiveSuppliers();

    /**
     * Retrieves single supplier details by primary key.
     *
     * @param id Supplier primary key
     * @return SupplierResponseDTO
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if not found
     */
    SupplierResponseDTO getSupplierById(Long id);

    /**
     * Registers a new supplier after verifying unique email constraints.
     *
     * @param requestDTO Inbound supplier creation payload
     * @return Created SupplierResponseDTO
     * @throws com.smartinventory.inventoryservice.exception.DuplicateResourceException if email already in use
     */
    SupplierResponseDTO createSupplier(SupplierRequestDTO requestDTO);

    /**
     * Updates an existing supplier's details.
     *
     * @param id         Supplier primary key
     * @param requestDTO Inbound supplier update payload
     * @return Updated SupplierResponseDTO
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if not found
     * @throws com.smartinventory.inventoryservice.exception.DuplicateResourceException if email belongs to another supplier
     */
    SupplierResponseDTO updateSupplier(Long id, SupplierRequestDTO requestDTO);

    /**
     * Deletes a supplier by primary key if no purchase or product records reference it.
     *
     * @param id Supplier primary key
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if not found
     */
    void deleteSupplier(Long id);

    /**
     * Updates supplier lifecycle status (ACTIVE or INACTIVE).
     *
     * @param id     Supplier primary key
     * @param status Target status ('ACTIVE' or 'INACTIVE')
     * @return Updated SupplierResponseDTO
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if not found
     * @throws IllegalArgumentException if status string is invalid
     */
    SupplierResponseDTO updateSupplierStatus(Long id, String status);
}
