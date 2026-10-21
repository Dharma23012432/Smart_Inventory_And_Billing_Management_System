package com.smartinventory.inventoryservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for updating inventory warehouse thresholds.
 * Strictly adheres to Section 3.3.3 of API_SPECIFICATION.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventorySettingsRequestDTO {

    @NotNull(message = "Reorder level is required")
    @Min(value = 0, message = "Reorder level cannot be negative")
    private Integer reorderLevel;

    @NotNull(message = "Max stock level is required")
    @Min(value = 1, message = "Max stock level must be at least 1")
    private Integer maxStockLevel;
}
