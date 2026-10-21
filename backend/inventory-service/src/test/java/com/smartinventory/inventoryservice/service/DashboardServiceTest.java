package com.smartinventory.inventoryservice.service;

import com.smartinventory.inventoryservice.dto.BillResponseDTO;
import com.smartinventory.inventoryservice.dto.DashboardSummaryDTO;
import com.smartinventory.inventoryservice.dto.InventoryResponseDTO;
import com.smartinventory.inventoryservice.entity.*;
import com.smartinventory.inventoryservice.entity.enums.BillStatus;
import com.smartinventory.inventoryservice.entity.enums.ProductStatus;
import com.smartinventory.inventoryservice.entity.enums.PurchaseStatus;
import com.smartinventory.inventoryservice.repository.*;
import com.smartinventory.inventoryservice.service.impl.DashboardServiceImpl;
import com.smartinventory.inventoryservice.service.mapper.BillMapper;
import com.smartinventory.inventoryservice.service.mapper.InventoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Unit Tests")
class DashboardServiceTest {

    @Mock
    private BillRepository billRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Spy
    private BillMapper billMapper = new BillMapper();

    @Spy
    private InventoryMapper inventoryMapper = new InventoryMapper();

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private Bill sampleBill;
    private Inventory sampleInventory;

    @BeforeEach
    void setUp() {
        Product product = Product.builder()
                .id(1L)
                .name("Wireless Mouse")
                .sku("ELEC-001")
                .unitPrice(new BigDecimal("500.00"))
                .build();

        sampleBill = Bill.builder()
                .id(1L)
                .billNumber("INV-2026-0001")
                .billDate(LocalDate.now())
                .totalAmount(new BigDecimal("1500.00"))
                .status(BillStatus.CONFIRMED)
                .build();

        sampleInventory = Inventory.builder()
                .id(10L)
                .product(product)
                .quantityOnHand(3)
                .reorderLevel(10)
                .build();
    }

    @Test
    @DisplayName("getDashboardSummary: Should aggregate today's sales, monthly sales, and entity counts")
    void getDashboardSummary_Success() {
        LocalDate today = LocalDate.now();
        when(billRepository.findByBillDateAndStatusNot(today, BillStatus.CANCELLED))
                .thenReturn(Collections.singletonList(sampleBill));
        when(billRepository.findByBillDateBetweenAndStatusNot(any(), any(), eq(BillStatus.CANCELLED)))
                .thenReturn(Collections.singletonList(sampleBill));
        when(inventoryRepository.countLowStock()).thenReturn(3L);
        when(purchaseRepository.countByStatus(PurchaseStatus.PENDING)).thenReturn(2L);
        when(productRepository.countByStatus(ProductStatus.ACTIVE)).thenReturn(142L);
        when(customerRepository.count()).thenReturn(89L);

        DashboardSummaryDTO result = dashboardService.getDashboardSummary();

        assertThat(result).isNotNull();
        assertThat(result.getTodaySales()).isEqualByComparingTo("1500.00");
        assertThat(result.getMonthlySales()).isEqualByComparingTo("1500.00");
        assertThat(result.getTotalBillsToday()).isEqualTo(1L);
        assertThat(result.getLowStockItemCount()).isEqualTo(3L);
        assertThat(result.getPendingPurchaseOrders()).isEqualTo(2L);
        assertThat(result.getTotalActiveProducts()).isEqualTo(142L);
        assertThat(result.getTotalCustomers()).isEqualTo(89L);
    }

    @Test
    @DisplayName("getRecentSales: Should return list of recent non-cancelled bills")
    void getRecentSales_Success() {
        when(billRepository.findByStatusNot(eq(BillStatus.CANCELLED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(sampleBill)));

        List<BillResponseDTO> result = dashboardService.getRecentSales(5);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBillNumber()).isEqualTo("INV-2026-0001");
    }

    @Test
    @DisplayName("getLowStockItems: Should return list of items with stock <= reorder level")
    void getLowStockItems_Success() {
        when(inventoryRepository.findAllLowStock()).thenReturn(Collections.singletonList(sampleInventory));

        List<InventoryResponseDTO> result = dashboardService.getLowStockItems();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductName()).isEqualTo("Wireless Mouse");
        assertThat(result.get(0).getIsLowStock()).isTrue();
    }
}
