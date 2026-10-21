package com.smartinventory.inventoryservice.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO representing a line item when creating a Bill.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillItemRequestDTO {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be strictly greater than 0")
    private Integer quantity;

    @DecimalMin(value = "0.0", message = "Discount percent cannot be negative")
    @DecimalMax(value = "99.99", message = "Discount percent must be less than 100")
    @Builder.Default
    private BigDecimal discountPercent = BigDecimal.ZERO;
}
