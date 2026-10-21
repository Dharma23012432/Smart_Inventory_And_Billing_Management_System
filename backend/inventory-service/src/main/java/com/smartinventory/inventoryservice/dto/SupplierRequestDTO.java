package com.smartinventory.inventoryservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for creating or updating a Supplier.
 * Strictly adheres to Section 3.4.3 of API_SPECIFICATION.md and DATABASE_SPECIFICATION.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierRequestDTO {

    @NotBlank(message = "Supplier name cannot be blank")
    @Size(max = 200, message = "Supplier name cannot exceed 200 characters")
    private String name;

    @Size(max = 150, message = "Contact person cannot exceed 150 characters")
    private String contactPerson;

    @NotBlank(message = "Supplier email cannot be blank")
    @Email(message = "Please provide a valid email address")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phone;

    private String address;

    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Status must be either ACTIVE or INACTIVE")
    private String status;
}
