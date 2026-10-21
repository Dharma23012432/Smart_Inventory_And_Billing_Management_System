package com.smartinventory.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary DTO of Inventory levels included within Product responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventorySummaryDTO {

    private Integer quantityOnHand;

    private Integer reorderLevel;

    private Integer maxStockLevel;

    private Boolean isLowStock;
}
