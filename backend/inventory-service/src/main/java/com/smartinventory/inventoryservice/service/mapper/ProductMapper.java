package com.smartinventory.inventoryservice.service.mapper;

import com.smartinventory.inventoryservice.dto.*;
import com.smartinventory.inventoryservice.entity.Category;
import com.smartinventory.inventoryservice.entity.Inventory;
import com.smartinventory.inventoryservice.entity.Product;
import com.smartinventory.inventoryservice.entity.enums.ProductStatus;
import org.springframework.stereotype.Component;

/**
 * Component for bidirectional mapping between Product entity and DTOs.
 * Uses manual mapping without reflection-based libraries for speed and type-safety.
 */
@Component
public class ProductMapper {

    /**
     * Converts a Product entity into a ProductResponseDTO.
     *
     * @param product Source product entity
     * @return Transformed ProductResponseDTO
     */
    public ProductResponseDTO toResponseDTO(Product product) {
        if (product == null) {
            return null;
        }

        CategorySummaryDTO categorySummary = null;
        if (product.getCategory() != null) {
            categorySummary = CategorySummaryDTO.builder()
                    .id(product.getCategory().getId())
                    .name(product.getCategory().getName())
                    .build();
        }

        SupplierSummaryDTO supplierSummary = null;
        if (product.getSupplierId() != null) {
            supplierSummary = SupplierSummaryDTO.builder()
                    .id(product.getSupplierId())
                    .name(null) // Populated if supplier name lookup is performed
                    .build();
        }

        InventorySummaryDTO inventorySummary = null;
        if (product.getInventory() != null) {
            Inventory inv = product.getInventory();
            inventorySummary = InventorySummaryDTO.builder()
                    .quantityOnHand(inv.getQuantityOnHand())
                    .reorderLevel(inv.getReorderLevel())
                    .maxStockLevel(inv.getMaxStockLevel())
                    .isLowStock(inv.isLowStock())
                    .build();
        }

        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .description(product.getDescription())
                .unitPrice(product.getUnitPrice())
                .purchasePrice(product.getPurchasePrice())
                .unitOfMeasure(product.getUnitOfMeasure())
                .status(product.getStatus() != null ? product.getStatus().name() : ProductStatus.ACTIVE.name())
                .category(categorySummary)
                .supplier(supplierSummary)
                .inventory(inventorySummary)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    /**
     * Converts ProductRequestDTO and Category entity into a new Product entity.
     *
     * @param requestDTO Inbound product creation payload
     * @param category   Resolved Category entity
     * @return New Product entity
     */
    public Product toEntity(ProductRequestDTO requestDTO, Category category) {
        if (requestDTO == null) {
            return null;
        }

        ProductStatus status = ProductStatus.ACTIVE;
        if (requestDTO.getStatus() != null) {
            try {
                status = ProductStatus.valueOf(requestDTO.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Default to ACTIVE
            }
        }

        return Product.builder()
                .category(category)
                .supplierId(requestDTO.getSupplierId())
                .name(requestDTO.getName() != null ? requestDTO.getName().trim() : null)
                .sku(requestDTO.getSku() != null ? requestDTO.getSku().trim().toUpperCase() : null)
                .description(requestDTO.getDescription() != null ? requestDTO.getDescription().trim() : null)
                .unitPrice(requestDTO.getUnitPrice())
                .purchasePrice(requestDTO.getPurchasePrice())
                .unitOfMeasure(requestDTO.getUnitOfMeasure() != null ? requestDTO.getUnitOfMeasure().trim().toUpperCase() : null)
                .status(status)
                .build();
    }

    /**
     * Updates an existing Product entity with values from ProductRequestDTO.
     *
     * @param requestDTO Inbound update payload
     * @param entity     Managed Product entity
     * @param category   Resolved Category entity
     */
    public void updateEntityFromDTO(ProductRequestDTO requestDTO, Product entity, Category category) {
        if (requestDTO == null || entity == null) {
            return;
        }

        entity.setCategory(category);
        entity.setSupplierId(requestDTO.getSupplierId());

        if (requestDTO.getName() != null) {
            entity.setName(requestDTO.getName().trim());
        }
        if (requestDTO.getSku() != null) {
            entity.setSku(requestDTO.getSku().trim().toUpperCase());
        }
        if (requestDTO.getDescription() != null) {
            entity.setDescription(requestDTO.getDescription().trim());
        }
        if (requestDTO.getUnitPrice() != null) {
            entity.setUnitPrice(requestDTO.getUnitPrice());
        }
        if (requestDTO.getPurchasePrice() != null) {
            entity.setPurchasePrice(requestDTO.getPurchasePrice());
        }
        if (requestDTO.getUnitOfMeasure() != null) {
            entity.setUnitOfMeasure(requestDTO.getUnitOfMeasure().trim().toUpperCase());
        }
        if (requestDTO.getStatus() != null) {
            try {
                entity.setStatus(ProductStatus.valueOf(requestDTO.getStatus().trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // Keep existing status if invalid
            }
        }
    }
}
