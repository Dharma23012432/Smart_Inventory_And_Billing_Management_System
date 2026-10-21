package com.smartinventory.inventoryservice.service.impl;

import com.smartinventory.inventoryservice.dto.InventoryValuationReportDTO;
import com.smartinventory.inventoryservice.dto.PurchaseReportDTO;
import com.smartinventory.inventoryservice.dto.SalesBreakdownDTO;
import com.smartinventory.inventoryservice.dto.SalesReportDTO;
import com.smartinventory.inventoryservice.entity.Bill;
import com.smartinventory.inventoryservice.entity.Inventory;
import com.smartinventory.inventoryservice.entity.Product;
import com.smartinventory.inventoryservice.entity.Purchase;
import com.smartinventory.inventoryservice.entity.enums.BillStatus;
import com.smartinventory.inventoryservice.entity.enums.PurchaseStatus;
import com.smartinventory.inventoryservice.repository.BillRepository;
import com.smartinventory.inventoryservice.repository.InventoryRepository;
import com.smartinventory.inventoryservice.repository.PurchaseRepository;
import com.smartinventory.inventoryservice.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Production implementation of ReportService.
 * Enforces financial audit calculations, layered architecture, constructor injection, and SLF4J logging.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final BillRepository billRepository;
    private final InventoryRepository inventoryRepository;
    private final PurchaseRepository purchaseRepository;

    public ReportServiceImpl(
            BillRepository billRepository,
            InventoryRepository inventoryRepository,
            PurchaseRepository purchaseRepository) {
        this.billRepository = billRepository;
        this.inventoryRepository = inventoryRepository;
        this.purchaseRepository = purchaseRepository;
    }

    @Override
    public SalesReportDTO getSalesReport(LocalDate startDate, LocalDate endDate, String groupBy) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        log.info("Generating sales revenue report from {} to {}, groupBy={}", start, end, groupBy);

        List<Bill> bills = billRepository.findByBillDateBetweenAndStatusNot(start, end, BillStatus.CANCELLED);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalDiscounts = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal netSales = BigDecimal.ZERO;

        Map<String, List<Bill>> groupedBills = new TreeMap<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (Bill bill : bills) {
            totalRevenue = totalRevenue.add(bill.getSubtotal() != null ? bill.getSubtotal() : BigDecimal.ZERO);
            totalDiscounts = totalDiscounts.add(bill.getDiscountAmount() != null ? bill.getDiscountAmount() : BigDecimal.ZERO);
            totalTax = totalTax.add(bill.getTaxAmount() != null ? bill.getTaxAmount() : BigDecimal.ZERO);
            netSales = netSales.add(bill.getTotalAmount() != null ? bill.getTotalAmount() : BigDecimal.ZERO);

            String key;
            if ("MONTHLY".equalsIgnoreCase(groupBy)) {
                key = bill.getBillDate().format(monthFormatter);
            } else {
                key = bill.getBillDate().toString();
            }
            groupedBills.computeIfAbsent(key, k -> new ArrayList<>()).add(bill);
        }

        List<SalesBreakdownDTO> breakdown = new ArrayList<>();
        for (Map.Entry<String, List<Bill>> entry : groupedBills.entrySet()) {
            List<Bill> periodBills = entry.getValue();
            BigDecimal periodTotal = periodBills.stream()
                    .map(Bill::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            breakdown.add(SalesBreakdownDTO.builder()
                    .period(entry.getKey())
                    .billCount((long) periodBills.size())
                    .totalAmount(periodTotal)
                    .build());
        }

        return SalesReportDTO.builder()
                .startDate(start)
                .endDate(end)
                .totalRevenue(totalRevenue.setScale(2, RoundingMode.HALF_UP))
                .totalDiscounts(totalDiscounts.setScale(2, RoundingMode.HALF_UP))
                .totalTax(totalTax.setScale(2, RoundingMode.HALF_UP))
                .netSales(netSales.setScale(2, RoundingMode.HALF_UP))
                .breakdown(breakdown)
                .build();
    }

    @Override
    public InventoryValuationReportDTO getInventoryValuationReport() {
        log.info("Generating inventory valuation audit report");

        List<Inventory> inventoryList = inventoryRepository.findAll();

        long totalStockQuantity = 0;
        BigDecimal totalCostValue = BigDecimal.ZERO;
        BigDecimal totalRetailValue = BigDecimal.ZERO;
        long lowStockCount = 0;

        for (Inventory inv : inventoryList) {
            int qty = inv.getQuantityOnHand() != null ? inv.getQuantityOnHand() : 0;
            totalStockQuantity += qty;

            if (inv.isLowStock()) {
                lowStockCount++;
            }

            Product product = inv.getProduct();
            if (product != null) {
                BigDecimal purchasePrice = product.getPurchasePrice() != null ? product.getPurchasePrice() : BigDecimal.ZERO;
                BigDecimal unitPrice = product.getUnitPrice() != null ? product.getUnitPrice() : BigDecimal.ZERO;

                totalCostValue = totalCostValue.add(purchasePrice.multiply(BigDecimal.valueOf(qty)));
                totalRetailValue = totalRetailValue.add(unitPrice.multiply(BigDecimal.valueOf(qty)));
            }
        }

        BigDecimal potentialProfit = totalRetailValue.subtract(totalCostValue).max(BigDecimal.ZERO);

        return InventoryValuationReportDTO.builder()
                .totalStockQuantity(totalStockQuantity)
                .totalCostValue(totalCostValue.setScale(2, RoundingMode.HALF_UP))
                .totalRetailValue(totalRetailValue.setScale(2, RoundingMode.HALF_UP))
                .potentialProfit(potentialProfit.setScale(2, RoundingMode.HALF_UP))
                .lowStockCount(lowStockCount)
                .build();
    }

    @Override
    public PurchaseReportDTO getPurchaseReport(LocalDate startDate, LocalDate endDate, Long supplierId) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        log.info("Generating procurement expense report from {} to {}, supplierId={}", start, end, supplierId);

        List<Purchase> purchases;
        if (supplierId != null) {
            purchases = purchaseRepository.findBySupplierIdAndPurchaseDateBetweenAndStatusNot(supplierId, start, end, PurchaseStatus.CANCELLED);
        } else {
            purchases = purchaseRepository.findByPurchaseDateBetweenAndStatusNot(start, end, PurchaseStatus.CANCELLED);
        }

        BigDecimal totalPurchases = BigDecimal.ZERO;
        Map<String, List<Purchase>> groupedPurchases = new TreeMap<>();

        for (Purchase purchase : purchases) {
            totalPurchases = totalPurchases.add(purchase.getTotalAmount() != null ? purchase.getTotalAmount() : BigDecimal.ZERO);
            String dateKey = purchase.getPurchaseDate().toString();
            groupedPurchases.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(purchase);
        }

        List<PurchaseReportDTO.PurchaseBreakdownDTO> breakdown = new ArrayList<>();
        for (Map.Entry<String, List<Purchase>> entry : groupedPurchases.entrySet()) {
            List<Purchase> dayPurchases = entry.getValue();
            BigDecimal dayTotal = dayPurchases.stream()
                    .map(Purchase::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            breakdown.add(PurchaseReportDTO.PurchaseBreakdownDTO.builder()
                    .period(entry.getKey())
                    .orderCount((long) dayPurchases.size())
                    .totalAmount(dayTotal)
                    .build());
        }

        return PurchaseReportDTO.builder()
                .startDate(start)
                .endDate(end)
                .totalPurchases(totalPurchases.setScale(2, RoundingMode.HALF_UP))
                .totalOrders((long) purchases.size())
                .breakdown(breakdown)
                .build();
    }
}
