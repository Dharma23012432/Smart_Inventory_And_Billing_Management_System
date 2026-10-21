package com.smartinventory.inventoryservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * General Data Transfer Object for Inventory update operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryRequestDTO {

    @Min(value = 0, message = "Reorder level cannot be negative")
    private Integer reorderLevel;

    @Min(value = 1, message = "Max stock level must be at least 1")
    private Integer maxStockLevel;

    @Min(value = 0, message = "Adjusted quantity cannot be negative")
    private Integer adjustedQuantity;

    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    private String reason;
}
