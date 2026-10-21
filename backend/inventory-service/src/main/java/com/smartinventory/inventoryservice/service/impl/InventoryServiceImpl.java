package com.smartinventory.inventoryservice.service.impl;

import com.smartinventory.inventoryservice.dto.InventoryAdjustmentRequestDTO;
import com.smartinventory.inventoryservice.dto.InventoryResponseDTO;
import com.smartinventory.inventoryservice.dto.InventorySettingsRequestDTO;
import com.smartinventory.inventoryservice.dto.PageResponse;
import com.smartinventory.inventoryservice.entity.Inventory;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.repository.InventoryRepository;
import com.smartinventory.inventoryservice.service.InventoryService;
import com.smartinventory.inventoryservice.service.mapper.InventoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Production implementation of InventoryService.
 * Enforces optimistic locking, non-negative stock invariants, and threshold validation.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    /**
     * Constructor injection for required dependencies.
     *
     * @param inventoryRepository Spring Data repository for Inventory
     * @param inventoryMapper     Manual entity-to-DTO mapper
     */
    public InventoryServiceImpl(
            InventoryRepository inventoryRepository,
            InventoryMapper inventoryMapper) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryMapper = inventoryMapper;
    }

    @Override
    public PageResponse<InventoryResponseDTO> getInventoryList(Boolean lowStockOnly, Pageable pageable) {
        log.info("Fetching inventory list: lowStockOnly={}, pageable={}", lowStockOnly, pageable);

        Page<Inventory> page;
        if (Boolean.TRUE.equals(lowStockOnly)) {
            page = inventoryRepository.findLowStock(pageable);
        } else {
            page = inventoryRepository.findAll(pageable);
        }

        log.debug("Retrieved {} inventory items across {} total pages",
                page.getNumberOfElements(), page.getTotalPages());

        return PageResponse.from(page, inventoryMapper::toResponseDTO);
    }

    @Override
    public InventoryResponseDTO getInventoryById(Long id) {
        log.info("Fetching inventory by ID: {}", id);

        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Inventory with ID: {} not found", id);
                    return new ResourceNotFoundException("Inventory", "id", id);
                });

        return inventoryMapper.toResponseDTO(inventory);
    }

    @Override
    public InventoryResponseDTO getInventoryByProductId(Long productId) {
        log.info("Fetching inventory for productId: {}", productId);

        Inventory inventory = findInventoryByProductIdOrThrow(productId);
        return inventoryMapper.toResponseDTO(inventory);
    }

    @Override
    public PageResponse<InventoryResponseDTO> getLowStockInventory(Pageable pageable) {
        log.info("Fetching low-stock inventory list with pageable: {}", pageable);

        Page<Inventory> page = inventoryRepository.findLowStock(pageable);
        log.debug("Found {} low-stock items across {} total pages",
                page.getNumberOfElements(), page.getTotalPages());

        return PageResponse.from(page, inventoryMapper::toResponseDTO);
    }

    @Override
    @Transactional
    public InventoryResponseDTO adjustStock(Long productId, InventoryAdjustmentRequestDTO requestDTO) {
        int newQuantity = requestDTO.getAdjustedQuantity();
        log.info("Manual stock adjustment for productId: {} to quantity: {}. Reason: '{}'",
                productId, newQuantity, requestDTO.getReason());

        if (newQuantity < 0) {
            log.warn("Attempted negative stock adjustment ({}) for productId: {}", newQuantity, productId);
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }

        Inventory inventory = findInventoryByProductIdOrThrow(productId);

        int previousQuantity = inventory.getQuantityOnHand();
        inventory.setQuantityOnHand(newQuantity);
        inventory.setLastUpdatedAt(Instant.now());

        // Save triggers JPA @Version check for optimistic locking concurrency control
        Inventory updatedInventory = inventoryRepository.save(inventory);

        log.info("Stock for productId {} updated successfully from {} to {}. Low stock: {}",
                productId, previousQuantity, newQuantity, updatedInventory.isLowStock());

        return inventoryMapper.toResponseDTO(updatedInventory);
    }

    @Override
    @Transactional
    public InventoryResponseDTO updateReorderSettings(Long productId, InventorySettingsRequestDTO requestDTO) {
        int reorderLevel = requestDTO.getReorderLevel();
        int maxStockLevel = requestDTO.getMaxStockLevel();

        log.info("Updating inventory settings for productId {}: reorderLevel={}, maxStockLevel={}",
                productId, reorderLevel, maxStockLevel);

        validateStockThresholds(reorderLevel, maxStockLevel);

        Inventory inventory = findInventoryByProductIdOrThrow(productId);
        inventory.setReorderLevel(reorderLevel);
        inventory.setMaxStockLevel(maxStockLevel);
        inventory.setLastUpdatedAt(Instant.now());

        Inventory updatedInventory = inventoryRepository.save(inventory);
        log.info("Inventory settings for productId {} updated successfully", productId);

        return inventoryMapper.toResponseDTO(updatedInventory);
    }

    @Override
    @Transactional
    public InventoryResponseDTO updateReorderLevel(Long productId, Integer reorderLevel) {
        log.info("Updating reorderLevel for productId {} to {}", productId, reorderLevel);

        if (reorderLevel == null || reorderLevel < 0) {
            throw new IllegalArgumentException("Reorder level cannot be negative or null");
        }

        Inventory inventory = findInventoryByProductIdOrThrow(productId);
        validateStockThresholds(reorderLevel, inventory.getMaxStockLevel());

        inventory.setReorderLevel(reorderLevel);
        inventory.setLastUpdatedAt(Instant.now());

        Inventory updatedInventory = inventoryRepository.save(inventory);
        log.info("Reorder level for productId {} updated to {}", productId, reorderLevel);

        return inventoryMapper.toResponseDTO(updatedInventory);
    }

    @Override
    @Transactional
    public InventoryResponseDTO updateMaxStockLevel(Long productId, Integer maxStockLevel) {
        log.info("Updating maxStockLevel for productId {} to {}", productId, maxStockLevel);

        if (maxStockLevel == null || maxStockLevel <= 0) {
            throw new IllegalArgumentException("Max stock level must be strictly positive");
        }

        Inventory inventory = findInventoryByProductIdOrThrow(productId);
        validateStockThresholds(inventory.getReorderLevel(), maxStockLevel);

        inventory.setMaxStockLevel(maxStockLevel);
        inventory.setLastUpdatedAt(Instant.now());

        Inventory updatedInventory = inventoryRepository.save(inventory);
        log.info("Max stock level for productId {} updated to {}", productId, maxStockLevel);

        return inventoryMapper.toResponseDTO(updatedInventory);
    }

    // -----------------------------------------------------------------
    // Internal Helper Methods
    // -----------------------------------------------------------------

    private Inventory findInventoryByProductIdOrThrow(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> {
                    log.warn("Inventory record for productId: {} not found", productId);
                    return new ResourceNotFoundException("Inventory", "productId", productId);
                });
    }

    private void validateStockThresholds(int reorderLevel, int maxStockLevel) {
        if (reorderLevel < 0) {
            throw new IllegalArgumentException("Reorder level cannot be negative");
        }
        if (maxStockLevel <= 0) {
            throw new IllegalArgumentException("Maximum stock level must be greater than zero");
        }
        if (reorderLevel >= maxStockLevel) {
            log.warn("Invalid stock threshold: reorderLevel ({}) >= maxStockLevel ({})",
                    reorderLevel, maxStockLevel);
            throw new IllegalArgumentException(String.format(
                    "Reorder level (%d) must be strictly less than maximum stock level (%d)",
                    reorderLevel, maxStockLevel));
        }
    }
}
