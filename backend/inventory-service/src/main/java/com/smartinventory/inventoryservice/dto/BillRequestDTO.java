package com.smartinventory.inventoryservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for initializing a new customer sales bill (Draft).
 * Strictly adheres to Section 3.7.4 of API_SPECIFICATION.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillRequestDTO {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @DecimalMin(value = "0.0", message = "Discount amount cannot be negative")
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Tax percent cannot be negative")
    @Builder.Default
    private BigDecimal taxPercent = new BigDecimal("18.00");

    private String notes;

    @NotEmpty(message = "Bill must contain at least one line item")
    @Valid
    private List<BillItemRequestDTO> items;
}
