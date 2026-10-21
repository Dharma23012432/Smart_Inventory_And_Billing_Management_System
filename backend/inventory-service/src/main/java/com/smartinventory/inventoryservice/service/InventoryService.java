package com.smartinventory.inventoryservice.service;

import com.smartinventory.inventoryservice.dto.InventoryAdjustmentRequestDTO;
import com.smartinventory.inventoryservice.dto.InventoryResponseDTO;
import com.smartinventory.inventoryservice.dto.InventorySettingsRequestDTO;
import com.smartinventory.inventoryservice.dto.PageResponse;
import org.springframework.data.domain.Pageable;

/**
 * Service interface defining business operations for real-time Inventory management.
 * Strictly adheres to Section 3.3 of API_SPECIFICATION.md and DATABASE_SPECIFICATION.md.
 */
public interface InventoryService {

    /**
     * Retrieves paginated inventory records across products.
     * Optionally filters only items at or below reorder threshold.
     *
     * @param lowStockOnly If true, filters for quantityOnHand <= reorderLevel
     * @param pageable     Pagination and sorting parameters
     * @return Paginated PageResponse of InventoryResponseDTOs
     */
    PageResponse<InventoryResponseDTO> getInventoryList(Boolean lowStockOnly, Pageable pageable);

    /**
     * Retrieves an inventory record by its primary key.
     *
     * @param id Inventory primary key
     * @return InventoryResponseDTO
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if not found
     */
    InventoryResponseDTO getInventoryById(Long id);

    /**
     * Retrieves the inventory record associated with a given Product ID.
     *
     * @param productId Product primary key
     * @return InventoryResponseDTO
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if not found
     */
    InventoryResponseDTO getInventoryByProductId(Long productId);

    /**
     * Retrieves all products currently at or below their designated reorder threshold.
     *
     * @param pageable Pagination and sorting parameters
     * @return Paginated PageResponse of low-stock inventory records
     */
    PageResponse<InventoryResponseDTO> getLowStockInventory(Pageable pageable);

    /**
     * Performs a manual stock adjustment (reconciliation, shrinkage, damaged goods audit).
     * Enforces non-negative stock and optimistic concurrency locking.
     *
     * @param productId  Product primary key
     * @param requestDTO Adjustment payload containing new quantity and justification reason
     * @return Updated InventoryResponseDTO
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if inventory not found
     * @throws IllegalArgumentException if adjustedQuantity is negative
     */
    InventoryResponseDTO adjustStock(Long productId, InventoryAdjustmentRequestDTO requestDTO);

    /**
     * Updates warehouse reorder threshold and maximum storage capacity for a product.
     * Enforces reorderLevel < maxStockLevel business rule.
     *
     * @param productId  Product primary key
     * @param requestDTO Settings payload containing reorderLevel and maxStockLevel
     * @return Updated InventoryResponseDTO
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if inventory not found
     * @throws IllegalArgumentException if reorderLevel >= maxStockLevel
     */
    InventoryResponseDTO updateReorderSettings(Long productId, InventorySettingsRequestDTO requestDTO);

    /**
     * Updates only the reorder threshold for a product's inventory.
     *
     * @param productId    Product primary key
     * @param reorderLevel New reorder threshold
     * @return Updated InventoryResponseDTO
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if inventory not found
     * @throws IllegalArgumentException if reorderLevel is negative or >= maxStockLevel
     */
    InventoryResponseDTO updateReorderLevel(Long productId, Integer reorderLevel);

    /**
     * Updates only the maximum warehouse capacity for a product's inventory.
     *
     * @param productId     Product primary key
     * @param maxStockLevel New maximum stock capacity
     * @return Updated InventoryResponseDTO
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if inventory not found
     * @throws IllegalArgumentException if maxStockLevel <= reorderLevel
     */
    InventoryResponseDTO updateMaxStockLevel(Long productId, Integer maxStockLevel);
}
