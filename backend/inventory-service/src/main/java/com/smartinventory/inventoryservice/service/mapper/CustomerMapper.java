package com.smartinventory.inventoryservice.service.mapper;

import com.smartinventory.inventoryservice.dto.CustomerRequestDTO;
import com.smartinventory.inventoryservice.dto.CustomerResponseDTO;
import com.smartinventory.inventoryservice.entity.Customer;
import com.smartinventory.inventoryservice.entity.enums.CustomerStatus;
import org.springframework.stereotype.Component;

/**
 * Component for bidirectional mapping between Customer entity and DTOs.
 * Uses manual reflection-free mapping for maximum performance and type safety.
 */
@Component
public class CustomerMapper {

    /**
     * Converts a Customer entity into a CustomerResponseDTO.
     *
     * @param customer Source customer entity
     * @return CustomerResponseDTO
     */
    public CustomerResponseDTO toResponseDTO(Customer customer) {
        if (customer == null) {
            return null;
        }

        return CustomerResponseDTO.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .status(customer.getStatus() != null ? customer.getStatus().name() : CustomerStatus.ACTIVE.name())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    /**
     * Converts a CustomerRequestDTO into a new Customer entity.
     *
     * @param requestDTO Inbound creation payload
     * @return New Customer entity
     */
    public Customer toEntity(CustomerRequestDTO requestDTO) {
        if (requestDTO == null) {
            return null;
        }

        CustomerStatus status = CustomerStatus.ACTIVE;
        if (requestDTO.getStatus() != null) {
            try {
                status = CustomerStatus.valueOf(requestDTO.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Default to ACTIVE
            }
        }

        return Customer.builder()
                .name(requestDTO.getName() != null ? requestDTO.getName().trim() : null)
                .email(requestDTO.getEmail() != null && !requestDTO.getEmail().trim().isEmpty() ? requestDTO.getEmail().trim().toLowerCase() : null)
                .phone(requestDTO.getPhone() != null && !requestDTO.getPhone().trim().isEmpty() ? requestDTO.getPhone().trim() : null)
                .address(requestDTO.getAddress() != null && !requestDTO.getAddress().trim().isEmpty() ? requestDTO.getAddress().trim() : null)
                .status(status)
                .build();
    }

    /**
     * Updates an existing Customer entity with values from a CustomerRequestDTO.
     *
     * @param requestDTO Inbound update payload
     * @param entity     Managed Customer entity
     */
    public void updateEntityFromDTO(CustomerRequestDTO requestDTO, Customer entity) {
        if (requestDTO == null || entity == null) {
            return;
        }

        if (requestDTO.getName() != null) {
            entity.setName(requestDTO.getName().trim());
        }
        if (requestDTO.getEmail() != null) {
            entity.setEmail(requestDTO.getEmail().trim().isEmpty() ? null : requestDTO.getEmail().trim().toLowerCase());
        }
        if (requestDTO.getPhone() != null) {
            entity.setPhone(requestDTO.getPhone().trim().isEmpty() ? null : requestDTO.getPhone().trim());
        }
        if (requestDTO.getAddress() != null) {
            entity.setAddress(requestDTO.getAddress().trim().isEmpty() ? null : requestDTO.getAddress().trim());
        }
        if (requestDTO.getStatus() != null) {
            try {
                entity.setStatus(CustomerStatus.valueOf(requestDTO.getStatus().trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Retain existing status
            }
        }
    }
}
