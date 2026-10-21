package com.smartinventory.inventoryservice.controller;

import com.smartinventory.inventoryservice.dto.InventoryValuationReportDTO;
import com.smartinventory.inventoryservice.dto.PurchaseReportDTO;
import com.smartinventory.inventoryservice.dto.SalesReportDTO;
import com.smartinventory.inventoryservice.exception.GlobalExceptionHandler;
import com.smartinventory.inventoryservice.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("ReportController WebMvc Tests")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    @DisplayName("GET /api/v1/reports/sales: Should return 200 with sales report")
    void getSalesReport_Success() throws Exception {
        SalesReportDTO response = SalesReportDTO.builder()
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 19))
                .totalRevenue(new BigDecimal("245000.00"))
                .totalDiscounts(new BigDecimal("12500.00"))
                .totalTax(new BigDecimal("41670.00"))
                .netSales(new BigDecimal("274170.00"))
                .breakdown(Collections.emptyList())
                .build();

        when(reportService.getSalesReport(any(), any(), eq("DAILY"))).thenReturn(response);

        mockMvc.perform(get("/api/v1/reports/sales")
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-19")
                        .param("groupBy", "DAILY")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Sales report generated")))
                .andExpect(jsonPath("$.data.netSales", is(274170.00)));
    }

    @Test
    @DisplayName("GET /api/v1/reports/inventory: Should return 200 with inventory valuation")
    void getInventoryValuationReport_Success() throws Exception {
        InventoryValuationReportDTO response = InventoryValuationReportDTO.builder()
                .totalStockQuantity(2450L)
                .totalCostValue(new BigDecimal("845200.00"))
                .totalRetailValue(new BigDecimal("1240000.00"))
                .potentialProfit(new BigDecimal("394800.00"))
                .lowStockCount(3L)
                .build();

        when(reportService.getInventoryValuationReport()).thenReturn(response);

        mockMvc.perform(get("/api/v1/reports/inventory")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Inventory valuation report generated")))
                .andExpect(jsonPath("$.data.totalStockQuantity", is(2450)))
                .andExpect(jsonPath("$.data.potentialProfit", is(394800.00)));
    }

    @Test
    @DisplayName("GET /api/v1/reports/purchases: Should return 200 with procurement report")
    void getPurchaseReport_Success() throws Exception {
        PurchaseReportDTO response = PurchaseReportDTO.builder()
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 19))
                .totalPurchases(new BigDecimal("12400.00"))
                .totalOrders(1L)
                .breakdown(Collections.emptyList())
                .build();

        when(reportService.getPurchaseReport(any(), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/reports/purchases")
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-19")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Procurement report generated")))
                .andExpect(jsonPath("$.data.totalPurchases", is(12400.00)));
    }
}
