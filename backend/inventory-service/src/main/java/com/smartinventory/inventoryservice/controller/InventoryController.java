package com.smartinventory.inventoryservice.controller;

import com.smartinventory.inventoryservice.dto.*;
import com.smartinventory.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing Inventory management endpoints.
 * Strictly adheres to Section 3.3 of API_SPECIFICATION.md with consistent /api/v1 prefix.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Constructor injection for InventoryService.
     *
     * @param inventoryService Inventory business operations service
     */
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * GET /api/v1/inventory
     * Retrieves paginated inventory records across products.
     *
     * @param lowStockOnly If true, filters for quantityOnHand <= reorderLevel
     * @param page         Page index (0-based, default: 0)
     * @param size         Page size (default: 10)
     * @param sortBy       Sort field (default: "id")
     * @param direction    Sort direction (ASC or DESC, default: "ASC")
     * @return Standardized ApiResponse wrapping paginated InventoryResponseDTOs
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponseDTO>>> getInventoryList(
            @RequestParam(required = false, defaultValue = "false") Boolean lowStockOnly,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String direction) {

        log.info("REST request to get inventory list: lowStockOnly={}, page={}, size={}",
                lowStockOnly, page, size);

        Sort.Direction sortDirection = "DESC".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        PageResponse<InventoryResponseDTO> response = inventoryService.getInventoryList(lowStockOnly, pageable);
        return ResponseEntity.ok(ApiResponse.success("Inventory retrieved successfully", response));
    }

    /**
     * GET /api/v1/inventory/{id}
     * Retrieves inventory details by inventory primary key.
     *
     * @param id Inventory primary key
     * @return Standardized ApiResponse wrapping InventoryResponseDTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryResponseDTO>> getInventoryById(@PathVariable Long id) {
        log.info("REST request to get inventory with ID: {}", id);
        InventoryResponseDTO response = inventoryService.getInventoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Inventory retrieved successfully", response));
    }

    /**
     * GET /api/v1/inventory/product/{productId}
     * Retrieves stock levels for a specific product.
     *
     * @param productId Product primary key
     * @return Standardized ApiResponse wrapping InventoryResponseDTO
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponseDTO>> getInventoryByProductId(@PathVariable Long productId) {
        log.info("REST request to get inventory for productId: {}", productId);
        InventoryResponseDTO response = inventoryService.getInventoryByProductId(productId);
        return ResponseEntity.ok(ApiResponse.success("Inventory retrieved successfully", response));
    }

    /**
     * GET /api/v1/inventory/low-stock
     * Dedicated convenience endpoint to list all items at or below reorder threshold.
     *
     * @param page Page index (0-based, default: 0)
     * @param size Page size (default: 10)
     * @return Standardized ApiResponse wrapping paginated low-stock InventoryResponseDTOs
     */
    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponseDTO>>> getLowStockInventory(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {

        log.info("REST request to get low stock inventory: page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size);

        PageResponse<InventoryResponseDTO> response = inventoryService.getLowStockInventory(pageable);
        return ResponseEntity.ok(ApiResponse.success("Low-stock inventory retrieved successfully", response));
    }

    /**
     * POST /api/v1/inventory/product/{productId}/adjust
     * Performs a manual stock reconciliation or correction.
     *
     * @param productId  Product primary key
     * @param requestDTO Validated stock adjustment payload
     * @return Standardized ApiResponse wrapping updated InventoryResponseDTO
     */
    @PostMapping("/product/{productId}/adjust")
    public ResponseEntity<ApiResponse<InventoryResponseDTO>> adjustStock(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryAdjustmentRequestDTO requestDTO) {

        log.info("REST request to adjust stock for productId: {} to {}", productId, requestDTO.getAdjustedQuantity());
        InventoryResponseDTO response = inventoryService.adjustStock(productId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Stock adjusted successfully", response));
    }

    /**
     * PUT /api/v1/inventory/product/{productId}/settings
     * Updates warehouse reorder threshold and max stock level.
     *
     * @param productId  Product primary key
     * @param requestDTO Validated settings payload
     * @return Standardized ApiResponse wrapping updated InventoryResponseDTO
     */
    @PutMapping("/product/{productId}/settings")
    public ResponseEntity<ApiResponse<InventoryResponseDTO>> updateReorderSettings(
            @PathVariable Long productId,
            @Valid @RequestBody InventorySettingsRequestDTO requestDTO) {

        log.info("REST request to update inventory settings for productId: {}", productId);
        InventoryResponseDTO response = inventoryService.updateReorderSettings(productId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Inventory settings updated successfully", response));
    }

    /**
     * PATCH /api/v1/inventory/product/{productId}/reorder-level
     * Updates only the reorder threshold.
     *
     * @param productId    Product primary key
     * @param reorderLevel New reorder threshold
     * @return Standardized ApiResponse wrapping updated InventoryResponseDTO
     */
    @PatchMapping("/product/{productId}/reorder-level")
    public ResponseEntity<ApiResponse<InventoryResponseDTO>> updateReorderLevel(
            @PathVariable Long productId,
            @RequestParam Integer reorderLevel) {

        log.info("REST request to update reorder level for productId: {} to {}", productId, reorderLevel);
        InventoryResponseDTO response = inventoryService.updateReorderLevel(productId, reorderLevel);
        return ResponseEntity.ok(ApiResponse.success("Reorder level updated successfully", response));
    }

    /**
     * PATCH /api/v1/inventory/product/{productId}/max-stock-level
     * Updates only the maximum warehouse capacity.
     *
     * @param productId     Product primary key
     * @param maxStockLevel New maximum stock capacity
     * @return Standardized ApiResponse wrapping updated InventoryResponseDTO
     */
    @PatchMapping("/product/{productId}/max-stock-level")
    public ResponseEntity<ApiResponse<InventoryResponseDTO>> updateMaxStockLevel(
            @PathVariable Long productId,
            @RequestParam Integer maxStockLevel) {

        log.info("REST request to update max stock level for productId: {} to {}", productId, maxStockLevel);
        InventoryResponseDTO response = inventoryService.updateMaxStockLevel(productId, maxStockLevel);
        return ResponseEntity.ok(ApiResponse.success("Max stock level updated successfully", response));
    }
}
