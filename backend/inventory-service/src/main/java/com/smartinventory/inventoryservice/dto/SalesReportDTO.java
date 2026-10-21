package com.smartinventory.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Aggregated sales revenue report DTO.
 * Strictly adheres to Section 3.11.1 of API_SPECIFICATION.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportDTO {

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal totalRevenue;

    private BigDecimal totalDiscounts;

    private BigDecimal totalTax;

    private BigDecimal netSales;

    private List<SalesBreakdownDTO> breakdown;
}
