package com.smartinventory.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Breakdown element in a sales report grouped by period or dimension.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesBreakdownDTO {

    private String period;

    private Long billCount;

    private BigDecimal totalAmount;

    public BigDecimal getRevenue() {
        return totalAmount;
    }
}
