package com.smartinventory.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Data Transfer Object representing a Product in API responses.
 * Encapsulates core product metadata along with nested category and inventory summaries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDTO {

    private Long id;

    private String name;

    private String sku;

    private String description;

    private BigDecimal unitPrice;

    private BigDecimal purchasePrice;

    private String unitOfMeasure;

    private String status;

    private CategorySummaryDTO category;

    private SupplierSummaryDTO supplier;

    private InventorySummaryDTO inventory;

    private Instant createdAt;

    private Instant updatedAt;
}
