package com.smartinventory.inventoryservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating a new Purchase order.
 * Strictly adheres to Section 3.6.3 of API_SPECIFICATION.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestDTO {

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    private LocalDate purchaseDate;

    private String notes;

    @NotEmpty(message = "Purchase order must contain at least one line item")
    @Valid
    private List<PurchaseItemRequestDTO> items;
}
