package com.smartinventory.inventoryservice.service.impl;

import com.smartinventory.inventoryservice.dto.*;
import com.smartinventory.inventoryservice.entity.*;
import com.smartinventory.inventoryservice.entity.enums.BillStatus;
import com.smartinventory.inventoryservice.entity.enums.CustomerStatus;
import com.smartinventory.inventoryservice.event.EventPublisher;
import com.smartinventory.inventoryservice.event.LowStockEvent;
import com.smartinventory.inventoryservice.event.LowStockPayload;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.repository.*;
import com.smartinventory.inventoryservice.service.BillService;
import com.smartinventory.inventoryservice.service.mapper.BillMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Production implementation of BillService.
 * Enforces business rules for sales calculations, sequential bill numbers,
 * atomic stock deductions with optimistic locking, low-stock event publication,
 * layered architecture, constructor injection, and SLF4J logging.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class BillServiceImpl implements BillService {

    private final BillRepository billRepository;
    private final BillItemRepository billItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final SupplierRepository supplierRepository;
    private final EventPublisher eventPublisher;
    private final BillMapper billMapper;

    /**
     * Constructor injection for all required dependencies.
     */
    public BillServiceImpl(
            BillRepository billRepository,
            BillItemRepository billItemRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            SupplierRepository supplierRepository,
            EventPublisher eventPublisher,
            BillMapper billMapper) {
        this.billRepository = billRepository;
        this.billItemRepository = billItemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.supplierRepository = supplierRepository;
        this.eventPublisher = eventPublisher;
        this.billMapper = billMapper;
    }

    @Override
    public PageResponse<BillResponseDTO> getBills(
            Long customerId,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            Boolean invoiceSent,
            Pageable pageable) {

        log.info("Fetching bills page: customerId={}, status='{}', startDate={}, endDate={}, invoiceSent={}, pageable={}",
                customerId, status, startDate, endDate, invoiceSent, pageable);

        final BillStatus filterStatus;
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            try {
                filterStatus = BillStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid bill status filter provided: '{}'", status);
                throw new IllegalArgumentException("Invalid status value: " + status + ". Allowed values: DRAFT, CONFIRMED, PAID, CANCELLED");
            }
        } else {
            filterStatus = null;
        }

        Specification<Bill> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }

            if (filterStatus != null) {
                predicates.add(cb.equal(root.get("status"), filterStatus));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("billDate"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("billDate"), endDate));
            }

            if (invoiceSent != null) {
                predicates.add(cb.equal(root.get("invoiceSent"), invoiceSent));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Bill> page = billRepository.findAll(spec, pageable);
        return PageResponse.from(page, billMapper::toResponseDTO);
    }

    @Override
    public BillResponseDTO getBillById(Long id) {
        log.info("Fetching bill by ID: {}", id);

        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with ID: " + id));

        return billMapper.toResponseDTO(bill);
    }

    @Override
    public BillResponseDTO getBillByBillNumber(String billNumber) {
        log.info("Fetching bill by bill number: {}", billNumber);

        Bill bill = billRepository.findByBillNumber(billNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with bill number: " + billNumber));

        return billMapper.toResponseDTO(bill);
    }

    @Override
    @Transactional
    public BillResponseDTO createBill(BillRequestDTO requestDTO) {
        log.info("Creating sales bill (DRAFT) for customer ID: {} with {} items",
                requestDTO.getCustomerId(), requestDTO.getItems() != null ? requestDTO.getItems().size() : 0);

        // 1. Validate customer existence and ACTIVE status
        Customer customer = customerRepository.findById(requestDTO.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + requestDTO.getCustomerId()));

        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot create bill for inactive customer: " + customer.getName());
        }

        // 2. Validate line items and check duplicate products
        Set<Long> productIds = new HashSet<>();
        for (BillItemRequestDTO itemDTO : requestDTO.getItems()) {
            if (!productIds.add(itemDTO.getProductId())) {
                throw new IllegalArgumentException("Duplicate product ID found in bill line items: " + itemDTO.getProductId());
            }
        }

        // 3. Generate sequential bill number: INV-YYYY-XXXX
        String billNumber = generateBillNumber();

        Bill bill = Bill.builder()
                .billNumber(billNumber)
                .customer(customer)
                .billDate(LocalDate.now())
                .status(BillStatus.DRAFT)
                .invoiceSent(false)
                .notes(requestDTO.getNotes() != null ? requestDTO.getNotes().trim() : null)
                .items(new ArrayList<>())
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;

        // 4. Compute line items using active selling price from product catalogue
        for (BillItemRequestDTO itemDTO : requestDTO.getItems()) {
            Product product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + itemDTO.getProductId()));

            BigDecimal unitPrice = product.getUnitPrice();
            BigDecimal discountPercent = itemDTO.getDiscountPercent() != null ? itemDTO.getDiscountPercent() : BigDecimal.ZERO;

            BigDecimal itemBase = unitPrice.multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
            BigDecimal discountFactor = BigDecimal.valueOf(100)
                    .subtract(discountPercent)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            BigDecimal totalPrice = itemBase.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(totalPrice);

            BillItem billItem = BillItem.builder()
                    .product(product)
                    .quantity(itemDTO.getQuantity())
                    .unitPrice(unitPrice)
                    .discountPercent(discountPercent)
                    .totalPrice(totalPrice)
                    .build();

            bill.addItem(billItem);
        }

        // 5. Compute subtotal, discount, tax, and total amount
        BigDecimal discountAmount = requestDTO.getDiscountAmount() != null ? requestDTO.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal taxableAmount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);

        BigDecimal taxPercent = requestDTO.getTaxPercent() != null ? requestDTO.getTaxPercent() : new BigDecimal("18.00");
        BigDecimal taxAmount = taxableAmount.multiply(taxPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = taxableAmount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        bill.setSubtotal(subtotal);
        bill.setDiscountAmount(discountAmount);
        bill.setTaxAmount(taxAmount);
        bill.setTotalAmount(totalAmount);

        Bill savedBill = billRepository.save(bill);
        log.info("Bill created successfully in DRAFT status: billNumber={}, totalAmount={}", savedBill.getBillNumber(), savedBill.getTotalAmount());

        return billMapper.toResponseDTO(savedBill);
    }

    @Override
    @Transactional
    public BillConfirmResponseDTO confirmBill(Long id) {
        log.info("Confirming bill with ID: {}", id);

        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with ID: " + id));

        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new IllegalStateException("Cannot confirm bill with status: " + bill.getStatus() + ". Only DRAFT bills can be confirmed.");
        }

        // 1. Verify stock availability and deduct inventory atomically
        for (BillItem item : bill.getItems()) {
            Product product = item.getProduct();
            Inventory inventory = inventoryRepository.findByProductId(product.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found for product ID: " + product.getId()));

            if (inventory.getQuantityOnHand() < item.getQuantity()) {
                log.warn("Insufficient stock for product '{}' (SKU: {}). Available: {}, Requested: {}",
                        product.getName(), product.getSku(), inventory.getQuantityOnHand(), item.getQuantity());
                throw new IllegalStateException(String.format(
                        "Insufficient stock for product '%s' (SKU: %s). Available: %d, Requested: %d",
                        product.getName(), product.getSku(), inventory.getQuantityOnHand(), item.getQuantity()));
            }

            int newQuantity = inventory.getQuantityOnHand() - item.getQuantity();
            inventory.setQuantityOnHand(newQuantity);
            inventory.setLastUpdatedAt(Instant.now());
            inventoryRepository.save(inventory);

            log.info("Deducted {} unit(s) of product '{}' (SKU: {}). Remaining on hand: {}",
                    item.getQuantity(), product.getName(), product.getSku(), newQuantity);

            // 2. Check reorder level and publish LowStockDetected event if triggered
            if (newQuantity <= inventory.getReorderLevel()) {
                Supplier supplier = null;
                if (product.getSupplierId() != null) {
                    supplier = supplierRepository.findById(product.getSupplierId()).orElse(null);
                }

                LowStockPayload payload = LowStockPayload.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .sku(product.getSku())
                        .currentQuantity(newQuantity)
                        .reorderLevel(inventory.getReorderLevel())
                        .supplierId(supplier != null ? supplier.getId() : null)
                        .supplierName(supplier != null ? supplier.getName() : null)
                        .supplierEmail(supplier != null ? supplier.getEmail() : null)
                        .build();

                eventPublisher.publishLowStockEvent(LowStockEvent.of(payload));
            }
        }

        // 3. Mark bill as CONFIRMED
        bill.setStatus(BillStatus.CONFIRMED);
        Bill confirmedBill = billRepository.save(bill);
        log.info("Bill ID: {} ({}) successfully CONFIRMED", confirmedBill.getId(), confirmedBill.getBillNumber());

        return BillConfirmResponseDTO.builder()
                .id(confirmedBill.getId())
                .billNumber(confirmedBill.getBillNumber())
                .status(BillStatus.CONFIRMED)
                .totalAmount(confirmedBill.getTotalAmount())
                .build();
    }

    @Override
    @Transactional
    public BillResponseDTO cancelBill(Long id) {
        log.info("Cancelling bill with ID: {}", id);

        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with ID: " + id));

        if (bill.getStatus() == BillStatus.PAID) {
            throw new IllegalStateException("Bills in PAID status cannot be cancelled in V1. Please initiate a formal refund process.");
        }

        if (bill.getStatus() == BillStatus.CANCELLED) {
            throw new IllegalStateException("Bill is already CANCELLED.");
        }

        // Restock inventory if bill was previously CONFIRMED
        if (bill.getStatus() == BillStatus.CONFIRMED) {
            for (BillItem item : bill.getItems()) {
                Product product = item.getProduct();
                Inventory inventory = inventoryRepository.findByProductId(product.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found for product ID: " + product.getId()));

                int restockedQuantity = inventory.getQuantityOnHand() + item.getQuantity();
                inventory.setQuantityOnHand(restockedQuantity);
                inventory.setLastUpdatedAt(Instant.now());
                inventoryRepository.save(inventory);

                log.info("Restocked {} unit(s) of product '{}' (SKU: {}). Current on hand: {}",
                        item.getQuantity(), product.getName(), product.getSku(), restockedQuantity);
            }
        }

        bill.setStatus(BillStatus.CANCELLED);
        Bill cancelledBill = billRepository.save(bill);
        log.info("Bill ID: {} ({}) marked as CANCELLED", cancelledBill.getId(), cancelledBill.getBillNumber());

        return billMapper.toResponseDTO(cancelledBill);
    }

    /**
     * Generates the next sequential unique bill number adhering to format INV-YYYY-XXXX.
     *
     * @return Formatted invoice identifier string
     */
    private synchronized String generateBillNumber() {
        int currentYear = LocalDate.now().getYear();
        String prefix = String.format("INV-%d-", currentYear);

        Optional<String> maxBillNumberOpt = billRepository.findMaxBillNumberByPrefix(prefix);
        int nextSequence = 1;

        if (maxBillNumberOpt.isPresent()) {
            String maxBill = maxBillNumberOpt.get();
            try {
                String sequencePart = maxBill.substring(prefix.length());
                nextSequence = Integer.parseInt(sequencePart) + 1;
            } catch (Exception ex) {
                log.warn("Failed to parse sequence from max bill number '{}': {}", maxBill, ex.getMessage());
            }
        }

        return String.format("%s%04d", prefix, nextSequence);
    }
}
