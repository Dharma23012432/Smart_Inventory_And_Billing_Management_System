package com.smartinventory.inventoryservice.service.impl;

import com.smartinventory.inventoryservice.dto.BillResponseDTO;
import com.smartinventory.inventoryservice.dto.DashboardSummaryDTO;
import com.smartinventory.inventoryservice.dto.InventoryResponseDTO;
import com.smartinventory.inventoryservice.entity.Bill;
import com.smartinventory.inventoryservice.entity.enums.BillStatus;
import com.smartinventory.inventoryservice.entity.enums.ProductStatus;
import com.smartinventory.inventoryservice.entity.enums.PurchaseStatus;
import com.smartinventory.inventoryservice.repository.*;
import com.smartinventory.inventoryservice.service.DashboardService;
import com.smartinventory.inventoryservice.service.mapper.BillMapper;
import com.smartinventory.inventoryservice.service.mapper.InventoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Production implementation of DashboardService.
 * Enforces high-performance KPI aggregation, layered architecture, constructor injection, and SLF4J logging.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final BillRepository billRepository;
    private final InventoryRepository inventoryRepository;
    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final BillMapper billMapper;
    private final InventoryMapper inventoryMapper;

    public DashboardServiceImpl(
            BillRepository billRepository,
            InventoryRepository inventoryRepository,
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            BillMapper billMapper,
            InventoryMapper inventoryMapper) {
        this.billRepository = billRepository;
        this.inventoryRepository = inventoryRepository;
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.billMapper = billMapper;
        this.inventoryMapper = inventoryMapper;
    }

    @Override
    public DashboardSummaryDTO getDashboardSummary() {
        log.info("Calculating dashboard KPI summary");

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        // 1. Today's sales
        List<Bill> todayBills = billRepository.findByBillDateAndStatusNot(today, BillStatus.CANCELLED);
        BigDecimal todaySales = todayBills.stream()
                .filter(b -> b.getStatus() == BillStatus.CONFIRMED || b.getStatus() == BillStatus.PAID)
                .map(Bill::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Monthly sales
        List<Bill> monthBills = billRepository.findByBillDateBetweenAndStatusNot(startOfMonth, endOfMonth, BillStatus.CANCELLED);
        BigDecimal monthlySales = monthBills.stream()
                .filter(b -> b.getStatus() == BillStatus.CONFIRMED || b.getStatus() == BillStatus.PAID)
                .map(Bill::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Counts
        long totalBillsToday = todayBills.size();
        long lowStockItemCount = inventoryRepository.countLowStock();
        long pendingPurchaseOrders = purchaseRepository.countByStatus(PurchaseStatus.PENDING);
        long totalActiveProducts = productRepository.countByStatus(ProductStatus.ACTIVE);
        long totalCustomers = customerRepository.count();

        return DashboardSummaryDTO.builder()
                .todaySales(todaySales)
                .monthlySales(monthlySales)
                .totalBillsToday(totalBillsToday)
                .lowStockItemCount(lowStockItemCount)
                .pendingPurchaseOrders(pendingPurchaseOrders)
                .totalActiveProducts(totalActiveProducts)
                .totalCustomers(totalCustomers)
                .build();
    }

    @Override
    public List<BillResponseDTO> getRecentSales(int limit) {
        int effectiveLimit = limit > 0 ? limit : 5;
        log.info("Fetching {} recent sales transactions", effectiveLimit);

        return billRepository.findByStatusNot(
                        BillStatus.CANCELLED,
                        PageRequest.of(0, effectiveLimit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent()
                .stream()
                .map(billMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryResponseDTO> getLowStockItems() {
        log.info("Fetching low-stock alert items for dashboard");

        return inventoryRepository.findAllLowStock().stream()
                .map(inventoryMapper::toResponseDTO)
                .collect(Collectors.toList());
    }
}
