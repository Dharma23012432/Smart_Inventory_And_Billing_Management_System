package com.smartinventory.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO representing an individual line item in a customer sales bill.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillItemResponseDTO {

    private Long id;

    private Long productId;

    private String productName;

    private String sku;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal discountPercent;

    private BigDecimal totalPrice;
}
