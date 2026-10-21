package com.smartinventory.inventoryservice.controller;

import com.smartinventory.inventoryservice.dto.*;
import com.smartinventory.inventoryservice.service.BillService;
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
 * REST controller exposing customer sales and billing management endpoints.
 * Strictly adheres to Section 3.7 of API_SPECIFICATION.md with consistent /api/v1 prefix.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bills")
public class BillController {

    private final BillService billService;

    /**
     * Constructor injection for BillService.
     *
     * @param billService Bill business operations service
     */
    public BillController(BillService billService) {
        this.billService = billService;
    }

    /**
     * GET /api/v1/bills
     * Queries sales bills with filtering and pagination.
     *
     * @param customerId  Optional customer ID filter
     * @param status      Optional bill status filter (DRAFT, CONFIRMED, PAID, CANCELLED)
     * @param startDate   Optional lower bound bill date (YYYY-MM-DD)
     * @param endDate     Optional upper bound bill date (YYYY-MM-DD)
     * @param invoiceSent Optional email dispatch audit filter
     * @param page        Page index (0-based, default: 0)
     * @param size        Page size (default: 10)
     * @param sortBy      Sort property (default: "createdAt")
     * @param direction   Sort direction (ASC or DESC, default: "DESC")
     * @return Paginated bills wrapped in standardized ApiResponse envelope
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BillResponseDTO>>> getBills(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Boolean invoiceSent,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String direction) {

        log.info("REST request to get bills: customerId={}, status='{}', startDate={}, endDate={}, invoiceSent={}, page={}, size={}",
                customerId, status, startDate, endDate, invoiceSent, page, size);

        Sort.Direction sortDir = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));

        PageResponse<BillResponseDTO> response = billService.getBills(
                customerId, status, startDate, endDate, invoiceSent, pageable);

        return ResponseEntity.ok(ApiResponse.success("Bills retrieved successfully", response));
    }

    /**
     * GET /api/v1/bills/{id}
     * Retrieves full bill breakdown by ID.
     *
     * @param id Bill primary key
     * @return Standardized ApiResponse with bill details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BillResponseDTO>> getBillById(@PathVariable Long id) {
        log.info("REST request to get bill by ID: {}", id);

        BillResponseDTO response = billService.getBillById(id);
        return ResponseEntity.ok(ApiResponse.success("Bill retrieved successfully", response));
    }

    /**
     * GET /api/v1/bills/number/{billNumber}
     * Looks up invoice by customer-facing invoice code (e.g., INV-2026-0001).
     *
     * @param billNumber Invoice identifier
     * @return Standardized ApiResponse with bill details
     */
    @GetMapping("/number/{billNumber}")
    public ResponseEntity<ApiResponse<BillResponseDTO>> getBillByBillNumber(@PathVariable String billNumber) {
        log.info("REST request to get bill by number: {}", billNumber);

        BillResponseDTO response = billService.getBillByBillNumber(billNumber);
        return ResponseEntity.ok(ApiResponse.success("Bill retrieved successfully", response));
    }

    /**
     * POST /api/v1/bills
     * Initializes a customer sales bill with line items in DRAFT status.
     *
     * @param requestDTO Inbound creation payload
     * @return Standardized ApiResponse with created draft bill (HTTP 201)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BillResponseDTO>> createBill(
            @Valid @RequestBody BillRequestDTO requestDTO) {

        log.info("REST request to create bill for customer ID: {}", requestDTO.getCustomerId());

        BillResponseDTO response = billService.createBill(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bill created successfully in DRAFT status", response));
    }

    /**
     * PUT /api/v1/bills/{id}/confirm
     * Finalizes bill, verifies stock availability, deducts inventory, and evaluates low-stock triggers.
     *
     * @param id Bill primary key
     * @return Standardized ApiResponse with confirmation details
     */
    @PutMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<BillConfirmResponseDTO>> confirmBill(@PathVariable Long id) {
        log.info("REST request to confirm bill ID: {}", id);

        BillConfirmResponseDTO response = billService.confirmBill(id);
        return ResponseEntity.ok(ApiResponse.success("Bill confirmed and inventory deducted successfully", response));
    }

    /**
     * PUT /api/v1/bills/{id}/cancel
     * Cancels a bill. Restocks inventory if previously CONFIRMED.
     *
     * @param id Bill primary key
     * @return Standardized ApiResponse with cancelled bill
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BillResponseDTO>> cancelBill(@PathVariable Long id) {
        log.info("REST request to cancel bill ID: {}", id);

        BillResponseDTO response = billService.cancelBill(id);
        return ResponseEntity.ok(ApiResponse.success("Bill cancelled successfully", response));
    }
}
