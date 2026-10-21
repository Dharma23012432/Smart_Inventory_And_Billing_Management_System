package com.smartinventory.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard Data Transfer Object representing an Inventory record in responses.
 * Strictly adheres to Section 3.3.1 of API_SPECIFICATION.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponseDTO {

    private Long inventoryId;

    private Long productId;

    private String productName;

    private String sku;

    private Integer quantityOnHand;

    private Integer reorderLevel;

    private Integer maxStockLevel;

    private Boolean isLowStock;

    private Long version;

    private Instant lastUpdatedAt;
}
