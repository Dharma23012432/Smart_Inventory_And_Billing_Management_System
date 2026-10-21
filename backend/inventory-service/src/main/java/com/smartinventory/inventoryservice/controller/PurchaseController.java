package com.smartinventory.inventoryservice.controller;

import com.smartinventory.inventoryservice.dto.*;
import com.smartinventory.inventoryservice.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST controller exposing procurement Purchase Order endpoints.
 * Strictly adheres to Section 3.6 of API_SPECIFICATION.md with consistent /api/v1 prefix.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    /**
     * Constructor injection for PurchaseService.
     *
     * @param purchaseService Purchase business operations service
     */
    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    /**
     * GET /api/v1/purchases
     * Retrieves paginated and filtered purchase orders.
     *
     * @param supplierId Optional supplier ID filter
     * @param status     Optional status filter (PENDING, RECEIVED, CANCELLED)
     * @param startDate  Optional start date filter (YYYY-MM-DD)
     * @param endDate    Optional end date filter (YYYY-MM-DD)
     * @param page       Page number (0-based, default: 0)
     * @param size       Page size (default: 10)
     * @param sortBy     Sort field (default: "createdAt")
     * @param direction  Sort direction (ASC or DESC, default: "DESC")
     * @return Standardized ApiResponse with paginated purchase orders
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PurchaseResponseDTO>>> getPurchases(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String direction) {

        log.info("REST request to get purchases: supplierId={}, status='{}', startDate={}, endDate={}, page={}, size={}",
                supplierId, status, startDate, endDate, page, size);

        Sort.Direction sortDir = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));

        PageResponse<PurchaseResponseDTO> response = purchaseService.getPurchases(
                supplierId, status, startDate, endDate, pageable);

        return ResponseEntity.ok(ApiResponse.success("Purchases retrieved successfully", response));
    }

    /**
     * GET /api/v1/purchases/{id}
     * Retrieves a purchase order by ID including line items and product details.
     *
     * @param id Purchase order primary key
     * @return Standardized ApiResponse with purchase order details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseResponseDTO>> getPurchaseById(@PathVariable Long id) {
        log.info("REST request to get purchase order by ID: {}", id);

        PurchaseResponseDTO response = purchaseService.getPurchaseById(id);
        return ResponseEntity.ok(ApiResponse.success("Purchase order retrieved", response));
    }

    /**
     * POST /api/v1/purchases
     * Creates a new purchase order in PENDING status.
     *
     * @param requestDTO Inbound creation payload with supplier and line items
     * @return Standardized ApiResponse with created purchase order (HTTP 201)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseResponseDTO>> createPurchase(
            @Valid @RequestBody PurchaseRequestDTO requestDTO) {

        log.info("REST request to create purchase order for supplier ID: {}", requestDTO.getSupplierId());

        PurchaseResponseDTO response = purchaseService.createPurchase(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Purchase order created successfully", response));
    }

    /**
     * PUT /api/v1/purchases/{id}/receive
     * Marks a purchase order as RECEIVED, ingesting inventory stock and updating purchase price.
     *
     * @param id Purchase order primary key
     * @return Standardized ApiResponse with receive details
     */
    @PutMapping("/{id}/receive")
    public ResponseEntity<ApiResponse<PurchaseReceiveResponseDTO>> receivePurchase(@PathVariable Long id) {
        log.info("REST request to receive purchase order ID: {}", id);

        PurchaseReceiveResponseDTO response = purchaseService.receivePurchase(id);
        return ResponseEntity.ok(ApiResponse.success("Purchase order received and inventory updated successfully", response));
    }

    /**
     * PUT /api/v1/purchases/{id}/cancel
     * Cancels an unreceived PENDING purchase order.
     *
     * @param id Purchase order primary key
     * @return Standardized ApiResponse with cancelled purchase order
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<PurchaseResponseDTO>> cancelPurchase(@PathVariable Long id) {
        log.info("REST request to cancel purchase order ID: {}", id);

        PurchaseResponseDTO response = purchaseService.cancelPurchase(id);
        return ResponseEntity.ok(ApiResponse.success("Purchase order cancelled successfully", response));
    }
}
