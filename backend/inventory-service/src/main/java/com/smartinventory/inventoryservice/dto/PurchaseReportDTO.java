package com.smartinventory.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Procurement expense report DTO.
 * Strictly adheres to Section 3.11.3 of API_SPECIFICATION.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseReportDTO {

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal totalPurchases;

    private Long totalOrders;

    private List<PurchaseBreakdownDTO> breakdown;

    public BigDecimal getTotalExpense() {
        return totalPurchases;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseBreakdownDTO {
        private String period;
        private String supplierName;
        private Long orderCount;
        private BigDecimal totalAmount;

        public BigDecimal getTotalExpense() {
            return totalAmount;
        }
    }
}
