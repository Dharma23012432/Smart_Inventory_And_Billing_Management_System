package com.smartinventory.inventoryservice.controller;

import com.smartinventory.inventoryservice.dto.ApiResponse;
import com.smartinventory.inventoryservice.dto.BillResponseDTO;
import com.smartinventory.inventoryservice.dto.DashboardSummaryDTO;
import com.smartinventory.inventoryservice.dto.InventoryResponseDTO;
import com.smartinventory.inventoryservice.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing high-level business indicators and operational dashboards.
 * Strictly adheres to Section 3.10 of API_SPECIFICATION.md with consistent /api/v1 prefix.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * GET /api/v1/dashboard/summary
     * Provides high-level business indicators and KPIs for the main dashboard.
     *
     * @return Standardized ApiResponse with DashboardSummaryDTO
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryDTO>> getDashboardSummary() {
        log.info("REST request to get dashboard summary KPIs");

        DashboardSummaryDTO response = dashboardService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary retrieved", response));
    }

    /**
     * GET /api/v1/dashboard/recent-sales
     * Displays recent sales transactions.
     *
     * @param limit Maximum number of records to return (default: 5)
     * @return Standardized ApiResponse with list of recent bills
     */
    @GetMapping("/recent-sales")
    public ResponseEntity<ApiResponse<List<BillResponseDTO>>> getRecentSales(
            @RequestParam(required = false, defaultValue = "5") int limit) {

        log.info("REST request to get recent sales (limit={})", limit);

        List<BillResponseDTO> response = dashboardService.getRecentSales(limit);
        return ResponseEntity.ok(ApiResponse.success("Recent sales retrieved successfully", response));
    }

    /**
     * GET /api/v1/dashboard/low-stock-items
     * Highlights items needing immediate replenishment.
     *
     * @return Standardized ApiResponse with low stock inventory items
     */
    @GetMapping("/low-stock-items")
    public ResponseEntity<ApiResponse<List<InventoryResponseDTO>>> getLowStockItems() {
        log.info("REST request to get dashboard low stock items");

        List<InventoryResponseDTO> response = dashboardService.getLowStockItems();
        return ResponseEntity.ok(ApiResponse.success("Low stock alert items retrieved", response));
    }
}
