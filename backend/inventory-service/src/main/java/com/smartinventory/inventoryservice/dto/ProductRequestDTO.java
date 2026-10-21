package com.smartinventory.inventoryservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object for creating or updating a Product.
 * Contains comprehensive Bean Validation annotations adhering to DATABASE_SPECIFICATION.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequestDTO {

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private Long supplierId;

    @NotBlank(message = "Product name cannot be blank")
    @Size(max = 200, message = "Product name cannot exceed 200 characters")
    private String name;

    @NotBlank(message = "SKU cannot be blank")
    @Size(max = 100, message = "SKU cannot exceed 100 characters")
    private String sku;

    private String description;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be strictly greater than 0")
    private BigDecimal unitPrice;

    @NotNull(message = "Purchase price is required")
    @DecimalMin(value = "0.00", message = "Purchase price cannot be negative")
    private BigDecimal purchasePrice;

    @NotBlank(message = "Unit of measure cannot be blank")
    @Size(max = 50, message = "Unit of measure cannot exceed 50 characters")
    private String unitOfMeasure;

    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Status must be either ACTIVE or INACTIVE")
    private String status;

    @Min(value = 0, message = "Initial stock cannot be negative")
    private Integer initialStock;

    @Min(value = 0, message = "Reorder level cannot be negative")
    private Integer reorderLevel;

    @Min(value = 1, message = "Max stock level must be at least 1")
    private Integer maxStockLevel;
}
