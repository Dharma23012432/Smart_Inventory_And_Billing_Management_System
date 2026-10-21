package com.smartinventory.inventoryservice.service.impl;

import com.smartinventory.inventoryservice.dto.*;
import com.smartinventory.inventoryservice.entity.Inventory;
import com.smartinventory.inventoryservice.entity.Product;
import com.smartinventory.inventoryservice.entity.Purchase;
import com.smartinventory.inventoryservice.entity.PurchaseItem;
import com.smartinventory.inventoryservice.entity.Supplier;
import com.smartinventory.inventoryservice.entity.enums.PurchaseStatus;
import com.smartinventory.inventoryservice.entity.enums.SupplierStatus;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.repository.InventoryRepository;
import com.smartinventory.inventoryservice.repository.ProductRepository;
import com.smartinventory.inventoryservice.repository.PurchaseItemRepository;
import com.smartinventory.inventoryservice.repository.PurchaseRepository;
import com.smartinventory.inventoryservice.repository.SupplierRepository;
import com.smartinventory.inventoryservice.service.PurchaseService;
import com.smartinventory.inventoryservice.service.mapper.PurchaseMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Production implementation of PurchaseService.
 * Enforces business rules for procurement, stock ingestion, cost price updates,
 * layered architecture, constructor injection, and SLF4J logging.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final PurchaseMapper purchaseMapper;

    /**
     * Constructor injection for all required repositories and mappers.
     */
    public PurchaseServiceImpl(
            PurchaseRepository purchaseRepository,
            PurchaseItemRepository purchaseItemRepository,
            SupplierRepository supplierRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            PurchaseMapper purchaseMapper) {
        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.purchaseMapper = purchaseMapper;
    }

    @Override
    public PageResponse<PurchaseResponseDTO> getPurchases(
            Long supplierId,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        log.info("Fetching purchases with supplierId={}, status='{}', startDate={}, endDate={}, pageable={}",
                supplierId, status, startDate, endDate, pageable);

        final PurchaseStatus filterStatus;
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            try {
                filterStatus = PurchaseStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid purchase status filter provided: '{}'", status);
                throw new IllegalArgumentException("Invalid status value: " + status + ". Allowed values: PENDING, RECEIVED, CANCELLED");
            }
        } else {
            filterStatus = null;
        }

        Specification<Purchase> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (supplierId != null) {
                predicates.add(cb.equal(root.get("supplier").get("id"), supplierId));
            }

            if (filterStatus != null) {
                predicates.add(cb.equal(root.get("status"), filterStatus));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("purchaseDate"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("purchaseDate"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Purchase> page = purchaseRepository.findAll(spec, pageable);
        return PageResponse.from(page, purchaseMapper::toResponseDTO);
    }

    @Override
    public PurchaseResponseDTO getPurchaseById(Long id) {
        log.info("Fetching purchase order with ID: {}", id);

        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with ID: " + id));

        return purchaseMapper.toResponseDTO(purchase);
    }

    @Override
    @Transactional
    public PurchaseResponseDTO createPurchase(PurchaseRequestDTO requestDTO) {
        log.info("Creating purchase order for supplier ID: {} with {} items",
                requestDTO.getSupplierId(), requestDTO.getItems() != null ? requestDTO.getItems().size() : 0);

        // 1. Verify supplier existence and ACTIVE status
        Supplier supplier = supplierRepository.findById(requestDTO.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + requestDTO.getSupplierId()));

        if (supplier.getStatus() != SupplierStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot create purchase order for inactive supplier: " + supplier.getName());
        }

        // 2. Validate line items and ensure no duplicate products
        Set<Long> productIds = new HashSet<>();
        for (PurchaseItemRequestDTO itemDTO : requestDTO.getItems()) {
            if (!productIds.add(itemDTO.getProductId())) {
                throw new IllegalArgumentException("Duplicate product ID found in purchase line items: " + itemDTO.getProductId());
            }
        }

        // 3. Build Purchase header
        LocalDate purchaseDate = requestDTO.getPurchaseDate() != null ? requestDTO.getPurchaseDate() : LocalDate.now();

        Purchase purchase = Purchase.builder()
                .supplier(supplier)
                .purchaseDate(purchaseDate)
                .status(PurchaseStatus.PENDING)
                .notes(requestDTO.getNotes() != null ? requestDTO.getNotes().trim() : null)
                .items(new ArrayList<>())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 4. Validate each product and build line items
        for (PurchaseItemRequestDTO itemDTO : requestDTO.getItems()) {
            Product product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + itemDTO.getProductId()));

            BigDecimal totalCost = itemDTO.getUnitCost().multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
            totalAmount = totalAmount.add(totalCost);

            PurchaseItem purchaseItem = PurchaseItem.builder()
                    .product(product)
                    .quantity(itemDTO.getQuantity())
                    .unitCost(itemDTO.getUnitCost())
                    .totalCost(totalCost)
                    .build();

            purchase.addItem(purchaseItem);
        }

        purchase.setTotalAmount(totalAmount);

        Purchase savedPurchase = purchaseRepository.save(purchase);
        log.info("Purchase order created successfully with ID: {}, totalAmount: {}", savedPurchase.getId(), savedPurchase.getTotalAmount());

        return purchaseMapper.toResponseDTO(savedPurchase);
    }

    @Override
    @Transactional
    public PurchaseReceiveResponseDTO receivePurchase(Long id) {
        log.info("Receiving purchase order with ID: {}", id);

        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with ID: " + id));

        // Validate state
        if (purchase.getStatus() != PurchaseStatus.PENDING) {
            throw new IllegalStateException("Cannot receive purchase order with status: " + purchase.getStatus() + ". Only PENDING orders can be received.");
        }

        // Stock ingestion & purchase price updates within atomic transaction
        for (PurchaseItem item : purchase.getItems()) {
            Product product = item.getProduct();

            Inventory inventory = inventoryRepository.findByProductId(product.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found for product ID: " + product.getId()));

            int newQuantity = inventory.getQuantityOnHand() + item.getQuantity();
            inventory.setQuantityOnHand(newQuantity);
            inventory.setLastUpdatedAt(Instant.now());
            inventoryRepository.save(inventory);

            // Update product's purchase price to reflect current supplier cost
            product.setPurchasePrice(item.getUnitCost());
            productRepository.save(product);

            log.info("Updated inventory for product '{}' (SKU: {}): stock on hand increased by {} to {}",
                    product.getName(), product.getSku(), item.getQuantity(), newQuantity);
        }

        purchase.setStatus(PurchaseStatus.RECEIVED);
        Purchase updatedPurchase = purchaseRepository.save(purchase);

        Instant receivedAt = Instant.now();
        log.info("Purchase order ID: {} marked as RECEIVED at {}", updatedPurchase.getId(), receivedAt);

        return PurchaseReceiveResponseDTO.builder()
                .id(updatedPurchase.getId())
                .status(PurchaseStatus.RECEIVED)
                .receivedAt(receivedAt)
                .build();
    }

    @Override
    @Transactional
    public PurchaseResponseDTO cancelPurchase(Long id) {
        log.info("Cancelling purchase order with ID: {}", id);

        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with ID: " + id));

        if (purchase.getStatus() != PurchaseStatus.PENDING) {
            throw new IllegalStateException("Cannot cancel purchase order with status: " + purchase.getStatus() + ". Only PENDING orders can be cancelled.");
        }

        purchase.setStatus(PurchaseStatus.CANCELLED);
        Purchase updatedPurchase = purchaseRepository.save(purchase);
        log.info("Purchase order ID: {} successfully CANCELLED", updatedPurchase.getId());

        return purchaseMapper.toResponseDTO(updatedPurchase);
    }
}
