package com.smartinventory.inventoryservice.service;

import com.smartinventory.inventoryservice.dto.InventoryValuationReportDTO;
import com.smartinventory.inventoryservice.dto.PurchaseReportDTO;
import com.smartinventory.inventoryservice.dto.SalesReportDTO;

import java.time.LocalDate;

/**
 * Service interface defining audit, financial, and analytical report operations.
 * Strictly adheres to Section 3.11 of API_SPECIFICATION.md.
 */
public interface ReportService {

    /**
     * Generates aggregated sales revenue report.
     *
     * @param startDate Lower bound date
     * @param endDate   Upper bound date
     * @param groupBy   Aggregation dimension (DAILY, MONTHLY)
     * @return SalesReportDTO
     */
    SalesReportDTO getSalesReport(LocalDate startDate, LocalDate endDate, String groupBy);

    /**
     * Generates audit of capital tied up in stock.
     *
     * @return InventoryValuationReportDTO
     */
    InventoryValuationReportDTO getInventoryValuationReport();

    /**
     * Generates supplier procurement expenditure report.
     *
     * @param startDate  Lower bound date
     * @param endDate    Upper bound date
     * @param supplierId Optional supplier ID filter
     * @return PurchaseReportDTO
     */
    PurchaseReportDTO getPurchaseReport(LocalDate startDate, LocalDate endDate, Long supplierId);
}
