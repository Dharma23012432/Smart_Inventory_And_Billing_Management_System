package com.smartinventory.inventoryservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload detailing product low-stock conditions for notifications and reordering.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockPayload {

    private Long productId;

    private String productName;

    private String sku;

    private Integer currentQuantity;

    private Integer reorderLevel;

    private Long supplierId;

    private String supplierName;

    private String supplierEmail;
}
