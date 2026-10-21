package com.smartinventory.inventoryservice.service.mapper;

import com.smartinventory.inventoryservice.dto.InventoryResponseDTO;
import com.smartinventory.inventoryservice.entity.Inventory;
import org.springframework.stereotype.Component;

/**
 * Component for converting Inventory entities to DTO representations.
 * Uses manual reflection-free mapping for maximum performance and type safety.
 */
@Component
public class InventoryMapper {

    /**
     * Converts an Inventory entity into an InventoryResponseDTO.
     *
     * @param inventory Source Inventory entity
     * @return Transformed InventoryResponseDTO or null if source is null
     */
    public InventoryResponseDTO toResponseDTO(Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        Long productId = null;
        String productName = null;
        String sku = null;

        if (inventory.getProduct() != null) {
            productId = inventory.getProduct().getId();
            productName = inventory.getProduct().getName();
            sku = inventory.getProduct().getSku();
        }

        return InventoryResponseDTO.builder()
                .inventoryId(inventory.getId())
                .productId(productId)
                .productName(productName)
                .sku(sku)
                .quantityOnHand(inventory.getQuantityOnHand())
                .reorderLevel(inventory.getReorderLevel())
                .maxStockLevel(inventory.getMaxStockLevel())
                .isLowStock(inventory.isLowStock())
                .version(inventory.getVersion())
                .lastUpdatedAt(inventory.getLastUpdatedAt())
                .build();
    }
}
