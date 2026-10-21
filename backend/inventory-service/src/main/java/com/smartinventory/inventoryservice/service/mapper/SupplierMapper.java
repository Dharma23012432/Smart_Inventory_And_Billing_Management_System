package com.smartinventory.inventoryservice.service.mapper;

import com.smartinventory.inventoryservice.dto.SupplierRequestDTO;
import com.smartinventory.inventoryservice.dto.SupplierResponseDTO;
import com.smartinventory.inventoryservice.entity.Supplier;
import com.smartinventory.inventoryservice.entity.enums.SupplierStatus;
import org.springframework.stereotype.Component;

/**
 * Component for bidirectional mapping between Supplier entity and DTOs.
 * Uses manual reflection-free mapping for maximum performance and type safety.
 */
@Component
public class SupplierMapper {

    /**
     * Converts a Supplier entity into a SupplierResponseDTO.
     *
     * @param supplier Source supplier entity
     * @return SupplierResponseDTO
     */
    public SupplierResponseDTO toResponseDTO(Supplier supplier) {
        if (supplier == null) {
            return null;
        }

        return SupplierResponseDTO.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .contactPerson(supplier.getContactPerson())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .address(supplier.getAddress())
                .status(supplier.getStatus() != null ? supplier.getStatus().name() : SupplierStatus.ACTIVE.name())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }

    /**
     * Converts a SupplierRequestDTO into a new Supplier entity.
     *
     * @param requestDTO Inbound creation payload
     * @return New Supplier entity
     */
    public Supplier toEntity(SupplierRequestDTO requestDTO) {
        if (requestDTO == null) {
            return null;
        }

        SupplierStatus status = SupplierStatus.ACTIVE;
        if (requestDTO.getStatus() != null) {
            try {
                status = SupplierStatus.valueOf(requestDTO.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Default to ACTIVE
            }
        }

        return Supplier.builder()
                .name(requestDTO.getName() != null ? requestDTO.getName().trim() : null)
                .contactPerson(requestDTO.getContactPerson() != null ? requestDTO.getContactPerson().trim() : null)
                .email(requestDTO.getEmail() != null ? requestDTO.getEmail().trim().toLowerCase() : null)
                .phone(requestDTO.getPhone() != null ? requestDTO.getPhone().trim() : null)
                .address(requestDTO.getAddress() != null ? requestDTO.getAddress().trim() : null)
                .status(status)
                .build();
    }

    /**
     * Updates an existing Supplier entity with values from a SupplierRequestDTO.
     *
     * @param requestDTO Inbound update payload
     * @param entity     Managed Supplier entity
     */
    public void updateEntityFromDTO(SupplierRequestDTO requestDTO, Supplier entity) {
        if (requestDTO == null || entity == null) {
            return;
        }

        if (requestDTO.getName() != null) {
            entity.setName(requestDTO.getName().trim());
        }
        if (requestDTO.getContactPerson() != null) {
            entity.setContactPerson(requestDTO.getContactPerson().trim());
        }
        if (requestDTO.getEmail() != null) {
            entity.setEmail(requestDTO.getEmail().trim().toLowerCase());
        }
        if (requestDTO.getPhone() != null) {
            entity.setPhone(requestDTO.getPhone().trim());
        }
        if (requestDTO.getAddress() != null) {
            entity.setAddress(requestDTO.getAddress().trim());
        }
        if (requestDTO.getStatus() != null) {
            try {
                entity.setStatus(SupplierStatus.valueOf(requestDTO.getStatus().trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Retain existing status
            }
        }
    }
}
