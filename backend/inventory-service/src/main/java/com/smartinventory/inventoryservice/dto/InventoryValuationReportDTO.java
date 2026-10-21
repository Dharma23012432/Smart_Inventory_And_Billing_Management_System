package com.smartinventory.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Inventory valuation report DTO.
 * Strictly adheres to Section 3.11.2 of API_SPECIFICATION.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryValuationReportDTO {

    private Long totalStockQuantity;

    private BigDecimal totalCostValue;

    private BigDecimal totalRetailValue;

    private BigDecimal potentialProfit;

    private Long lowStockCount;
}
