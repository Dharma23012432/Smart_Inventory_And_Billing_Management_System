package com.smartinventory.inventoryservice.service;

import com.smartinventory.inventoryservice.dto.InventoryValuationReportDTO;
import com.smartinventory.inventoryservice.dto.PurchaseReportDTO;
import com.smartinventory.inventoryservice.dto.SalesReportDTO;
import com.smartinventory.inventoryservice.entity.*;
import com.smartinventory.inventoryservice.entity.enums.BillStatus;
import com.smartinventory.inventoryservice.entity.enums.PurchaseStatus;
import com.smartinventory.inventoryservice.repository.BillRepository;
import com.smartinventory.inventoryservice.repository.InventoryRepository;
import com.smartinventory.inventoryservice.repository.PurchaseRepository;
import com.smartinventory.inventoryservice.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService Unit Tests")
class ReportServiceTest {

    @Mock
    private BillRepository billRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private Bill sampleBill;
    private Inventory sampleInventory;
    private Purchase samplePurchase;

    @BeforeEach
    void setUp() {
        Product product = Product.builder()
                .id(1L)
                .name("Wireless Mouse")
                .sku("ELEC-001")
                .unitPrice(new BigDecimal("1000.00"))
                .purchasePrice(new BigDecimal("600.00"))
                .build();

        sampleBill = Bill.builder()
                .id(1L)
                .billNumber("INV-2026-0001")
                .billDate(LocalDate.now())
                .subtotal(new BigDecimal("1000.00"))
                .discountAmount(new BigDecimal("50.00"))
                .taxAmount(new BigDecimal("171.00"))
                .totalAmount(new BigDecimal("1121.00"))
                .status(BillStatus.CONFIRMED)
                .build();

        sampleInventory = Inventory.builder()
                .id(10L)
                .product(product)
                .quantityOnHand(10)
                .reorderLevel(5)
                .maxStockLevel(100)
                .build();

        samplePurchase = Purchase.builder()
                .id(5L)
                .purchaseDate(LocalDate.now())
                .totalAmount(new BigDecimal("6000.00"))
                .status(PurchaseStatus.RECEIVED)
                .build();
    }

    @Test
    @DisplayName("getSalesReport: Should compute revenue, discounts, tax, and net sales")
    void getSalesReport_Success() {
        LocalDate start = LocalDate.now().minusDays(10);
        LocalDate end = LocalDate.now();

        when(billRepository.findByBillDateBetweenAndStatusNot(eq(start), eq(end), eq(BillStatus.CANCELLED)))
                .thenReturn(Collections.singletonList(sampleBill));

        SalesReportDTO report = reportService.getSalesReport(start, end, "DAILY");

        assertThat(report).isNotNull();
        assertThat(report.getTotalRevenue()).isEqualByComparingTo("1000.00");
        assertThat(report.getTotalDiscounts()).isEqualByComparingTo("50.00");
        assertThat(report.getTotalTax()).isEqualByComparingTo("171.00");
        assertThat(report.getNetSales()).isEqualByComparingTo("1121.00");
        assertThat(report.getBreakdown()).hasSize(1);
    }

    @Test
    @DisplayName("getInventoryValuationReport: Should compute total cost, retail value, potential profit, and low stock count")
    void getInventoryValuationReport_Success() {
        when(inventoryRepository.findAll()).thenReturn(Collections.singletonList(sampleInventory));

        InventoryValuationReportDTO report = reportService.getInventoryValuationReport();

        assertThat(report).isNotNull();
        assertThat(report.getTotalStockQuantity()).isEqualTo(10L);
        // 10 * 600 = 6000.00 cost
        assertThat(report.getTotalCostValue()).isEqualByComparingTo("6000.00");
        // 10 * 1000 = 10000.00 retail
        assertThat(report.getTotalRetailValue()).isEqualByComparingTo("10000.00");
        // 10000 - 6000 = 4000.00 profit
        assertThat(report.getPotentialProfit()).isEqualByComparingTo("4000.00");
    }

    @Test
    @DisplayName("getPurchaseReport: Should compute total purchases and order count in period")
    void getPurchaseReport_Success() {
        LocalDate start = LocalDate.now().minusDays(10);
        LocalDate end = LocalDate.now();

        when(purchaseRepository.findByPurchaseDateBetweenAndStatusNot(eq(start), eq(end), eq(PurchaseStatus.CANCELLED)))
                .thenReturn(Collections.singletonList(samplePurchase));

        PurchaseReportDTO report = reportService.getPurchaseReport(start, end, null);

        assertThat(report).isNotNull();
        assertThat(report.getTotalPurchases()).isEqualByComparingTo("6000.00");
        assertThat(report.getTotalOrders()).isEqualTo(1L);
        assertThat(report.getBreakdown()).hasSize(1);
    }
}
