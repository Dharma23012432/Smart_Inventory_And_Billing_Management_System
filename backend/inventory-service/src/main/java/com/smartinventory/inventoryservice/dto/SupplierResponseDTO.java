package com.smartinventory.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object representing a Supplier in API responses.
 * Strictly adheres to Section 3.4 of API_SPECIFICATION.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponseDTO {

    private Long id;

    private String name;

    private String contactPerson;

    private String email;

    private String phone;

    private String address;

    private String status;

    private Instant createdAt;

    private Instant updatedAt;
}
