package com.smartinventory.inventoryservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for manual stock adjustments (stock take, damage, reconciliation).
 * Strictly adheres to Section 3.3.4 of API_SPECIFICATION.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAdjustmentRequestDTO {

    @NotNull(message = "Adjusted quantity is required")
    @Min(value = 0, message = "Adjusted quantity cannot be negative")
    private Integer adjustedQuantity;

    @NotBlank(message = "Adjustment reason cannot be blank")
    @Size(max = 255, message = "Adjustment reason cannot exceed 255 characters")
    private String reason;
}
