package com.smartinventory.inventoryservice.service;

import com.smartinventory.inventoryservice.dto.BillResponseDTO;
import com.smartinventory.inventoryservice.dto.DashboardSummaryDTO;
import com.smartinventory.inventoryservice.dto.InventoryResponseDTO;

import java.util.List;

/**
 * Service interface defining business dashboard and real-time operational KPI metrics.
 * Strictly adheres to Section 3.10 of API_SPECIFICATION.md.
 */
public interface DashboardService {

    /**
     * Retrieves high-level business indicator KPIs.
     *
     * @return DashboardSummaryDTO
     */
    DashboardSummaryDTO getDashboardSummary();

    /**
     * Retrieves the most recent sales transactions.
     *
     * @param limit Maximum number of records to return
     * @return List of recent BillResponseDTOs
     */
    List<BillResponseDTO> getRecentSales(int limit);

    /**
     * Retrieves items needing immediate procurement attention.
     *
     * @return List of low stock InventoryResponseDTOs
     */
    List<InventoryResponseDTO> getLowStockItems();
}
