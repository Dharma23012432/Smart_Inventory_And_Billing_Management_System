package com.smartinventory.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * High-level business indicator KPIs for the dashboard landing page.
 * Strictly adheres to Section 3.10.1 of API_SPECIFICATION.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {

    private BigDecimal todaySales;

    private BigDecimal monthlySales;

    private Long totalBillsToday;

    private Long lowStockItemCount;

    private Long pendingPurchaseOrders;

    private Long totalActiveProducts;

    private Long totalCustomers;
}
