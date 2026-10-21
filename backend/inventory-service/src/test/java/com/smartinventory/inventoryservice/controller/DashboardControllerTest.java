package com.smartinventory.inventoryservice.controller;

import com.smartinventory.inventoryservice.dto.BillResponseDTO;
import com.smartinventory.inventoryservice.dto.DashboardSummaryDTO;
import com.smartinventory.inventoryservice.dto.InventoryResponseDTO;
import com.smartinventory.inventoryservice.exception.GlobalExceptionHandler;
import com.smartinventory.inventoryservice.service.DashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("DashboardController WebMvc Tests")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @Test
    @DisplayName("GET /api/v1/dashboard/summary: Should return 200 with dashboard summary KPIs")
    void getDashboardSummary_Success() throws Exception {
        DashboardSummaryDTO summary = DashboardSummaryDTO.builder()
                .todaySales(new BigDecimal("14250.00"))
                .monthlySales(new BigDecimal("348200.00"))
                .totalBillsToday(18L)
                .lowStockItemCount(3L)
                .pendingPurchaseOrders(2L)
                .totalActiveProducts(142L)
                .totalCustomers(89L)
                .build();

        when(dashboardService.getDashboardSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Dashboard summary retrieved")))
                .andExpect(jsonPath("$.data.todaySales", is(14250.00)))
                .andExpect(jsonPath("$.data.totalBillsToday", is(18)))
                .andExpect(jsonPath("$.data.lowStockItemCount", is(3)));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/recent-sales: Should return 200 with recent sales")
    void getRecentSales_Success() throws Exception {
        BillResponseDTO bill = BillResponseDTO.builder()
                .id(1L)
                .billNumber("INV-2026-0001")
                .totalAmount(new BigDecimal("14250.00"))
                .build();

        when(dashboardService.getRecentSales(5)).thenReturn(Collections.singletonList(bill));

        mockMvc.perform(get("/api/v1/dashboard/recent-sales?limit=5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].billNumber", is("INV-2026-0001")));
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/low-stock-items: Should return 200 with low stock items")
    void getLowStockItems_Success() throws Exception {
        InventoryResponseDTO item = InventoryResponseDTO.builder()
                .inventoryId(1L)
                .productId(2L)
                .productName("Pilot Pen")
                .quantityOnHand(3)
                .isLowStock(true)
                .build();

        when(dashboardService.getLowStockItems()).thenReturn(Collections.singletonList(item));

        mockMvc.perform(get("/api/v1/dashboard/low-stock-items")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].productName", is("Pilot Pen")));
    }
}
