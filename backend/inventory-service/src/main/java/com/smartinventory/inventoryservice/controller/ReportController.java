package com.smartinventory.inventoryservice.controller;

import com.smartinventory.inventoryservice.dto.ApiResponse;
import com.smartinventory.inventoryservice.dto.InventoryValuationReportDTO;
import com.smartinventory.inventoryservice.dto.PurchaseReportDTO;
import com.smartinventory.inventoryservice.dto.SalesReportDTO;
import com.smartinventory.inventoryservice.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST controller exposing financial, analytical, and valuation reports.
 * Strictly adheres to Section 3.11 of API_SPECIFICATION.md with consistent /api/v1 prefix.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * GET /api/v1/reports/sales
     * Generates aggregated sales revenue report.
     *
     * @param startDate Lower bound date (YYYY-MM-DD)
     * @param endDate   Upper bound date (YYYY-MM-DD)
     * @param groupBy   Aggregation dimension (DAILY, MONTHLY, default: DAILY)
     * @return Standardized ApiResponse with SalesReportDTO
     */
    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<SalesReportDTO>> getSalesReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "DAILY") String groupBy) {

        log.info("REST request to generate sales revenue report: startDate={}, endDate={}, groupBy={}",
                startDate, endDate, groupBy);

        SalesReportDTO response = reportService.getSalesReport(startDate, endDate, groupBy);
        return ResponseEntity.ok(ApiResponse.success("Sales report generated", response));
    }

    /**
     * GET /api/v1/reports/inventory
     * Generates audit report of capital and retail value tied up in stock.
     *
     * @return Standardized ApiResponse with InventoryValuationReportDTO
     */
    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<InventoryValuationReportDTO>> getInventoryValuationReport() {
        log.info("REST request to generate inventory valuation report");

        InventoryValuationReportDTO response = reportService.getInventoryValuationReport();
        return ResponseEntity.ok(ApiResponse.success("Inventory valuation report generated", response));
    }

    /**
     * GET /api/v1/reports/purchases
     * Generates procurement and supplier expense report.
     *
     * @param startDate  Lower bound date (YYYY-MM-DD)
     * @param endDate    Upper bound date (YYYY-MM-DD)
     * @param supplierId Optional supplier filter
     * @return Standardized ApiResponse with PurchaseReportDTO
     */
    @GetMapping("/purchases")
    public ResponseEntity<ApiResponse<PurchaseReportDTO>> getPurchaseReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long supplierId) {

        log.info("REST request to generate procurement expense report: startDate={}, endDate={}, supplierId={}",
                startDate, endDate, supplierId);

        PurchaseReportDTO response = reportService.getPurchaseReport(startDate, endDate, supplierId);
        return ResponseEntity.ok(ApiResponse.success("Procurement report generated", response));
    }
}
