package com.smartinventory.inventoryservice.service;

import com.smartinventory.inventoryservice.dto.CustomerRequestDTO;
import com.smartinventory.inventoryservice.dto.CustomerResponseDTO;
import com.smartinventory.inventoryservice.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface defining business operations for Customer management.
 * Strictly adheres to Section 3.5 of API_SPECIFICATION.md.
 */
public interface CustomerService {

    /**
     * Retrieves a paginated list of customers matching optional search query and status filter.
     *
     * @param search   Optional search text across name, phone, or email
     * @param status   Optional status filter (e.g. ACTIVE, INACTIVE, ALL)
     * @param pageable Pagination and sorting parameters
     * @return Standardized PageResponse of CustomerResponseDTOs
     */
    PageResponse<CustomerResponseDTO> getCustomers(String search, String status, Pageable pageable);

    /**
     * Retrieves all active customers sorted by name for UI checkout selections.
     *
     * @return List of active CustomerResponseDTOs
     */
    List<CustomerResponseDTO> getAllActiveCustomers();

    /**
     * Retrieves single customer details by primary key.
     *
     * @param id Customer primary key
     * @return CustomerResponseDTO
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if not found
     */
    CustomerResponseDTO getCustomerById(Long id);

    /**
     * Registers a new customer profile.
     *
     * @param requestDTO Inbound customer creation payload
     * @return Created CustomerResponseDTO
     */
    CustomerResponseDTO createCustomer(CustomerRequestDTO requestDTO);

    /**
     * Updates an existing customer's contact details.
     *
     * @param id         Customer primary key
     * @param requestDTO Inbound customer update payload
     * @return Updated CustomerResponseDTO
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if not found
     */
    CustomerResponseDTO updateCustomer(Long id, CustomerRequestDTO requestDTO);

    /**
     * Deletes a customer by primary key if no billing records reference it.
     *
     * @param id Customer primary key
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if not found
     */
    void deleteCustomer(Long id);

    /**
     * Updates customer lifecycle status (ACTIVE or INACTIVE).
     *
     * @param id     Customer primary key
     * @param status Target status ('ACTIVE' or 'INACTIVE')
     * @return Updated CustomerResponseDTO
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if not found
     * @throws IllegalArgumentException if status string is invalid
     */
    CustomerResponseDTO updateCustomerStatus(Long id, String status);
}
