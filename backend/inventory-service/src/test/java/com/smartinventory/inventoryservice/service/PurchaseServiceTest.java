package com.smartinventory.inventoryservice.service;

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
import com.smartinventory.inventoryservice.service.impl.PurchaseServiceImpl;
import com.smartinventory.inventoryservice.service.mapper.PurchaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseService Unit Tests")
class PurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private PurchaseItemRepository purchaseItemRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Spy
    private PurchaseMapper purchaseMapper = new PurchaseMapper();

    @InjectMocks
    private PurchaseServiceImpl purchaseService;

    private Supplier testSupplier;
    private Product testProduct1;
    private Product testProduct2;
    private Inventory testInventory1;
    private Inventory testInventory2;
    private Purchase testPurchase;
    private PurchaseItem testItem1;

    @BeforeEach
    void setUp() {
        testSupplier = Supplier.builder()
                .id(1L)
                .name("TechSource Distribution Ltd")
                .email("orders@techsource.com")
                .status(SupplierStatus.ACTIVE)
                .build();

        testProduct1 = Product.builder()
                .id(1L)
                .name("Logitech Wireless Mouse M220 Silent")
                .sku("ELEC-LOG-M220")
                .unitPrice(new BigDecimal("799.00"))
                .purchasePrice(new BigDecimal("600.00"))
                .unitOfMeasure("PIECE")
                .build();

        testProduct2 = Product.builder()
                .id(2L)
                .name("Dell Pro Wireless Keyboard")
                .sku("ELEC-DEL-KB1")
                .unitPrice(new BigDecimal("1299.00"))
                .purchasePrice(new BigDecimal("950.00"))
                .unitOfMeasure("PIECE")
                .build();

        testInventory1 = Inventory.builder()
                .id(101L)
                .product(testProduct1)
                .quantityOnHand(10)
                .reorderLevel(5)
                .maxStockLevel(100)
                .lastUpdatedAt(Instant.now())
                .build();

        testInventory2 = Inventory.builder()
                .id(102L)
                .product(testProduct2)
                .quantityOnHand(15)
                .reorderLevel(5)
                .maxStockLevel(100)
                .lastUpdatedAt(Instant.now())
                .build();

        testPurchase = Purchase.builder()
                .id(1L)
                .supplier(testSupplier)
                .purchaseDate(LocalDate.now())
                .totalAmount(new BigDecimal("12400.00"))
                .status(PurchaseStatus.PENDING)
                .notes("Urgent stock replenishment")
                .items(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testItem1 = PurchaseItem.builder()
                .id(10L)
                .purchase(testPurchase)
                .product(testProduct1)
                .quantity(20)
                .unitCost(new BigDecimal("620.00"))
                .totalCost(new BigDecimal("12400.00"))
                .build();

        testPurchase.addItem(testItem1);
    }

    @Test
    @DisplayName("getPurchases: Should return paginated list of purchase orders")
    void getPurchases_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Purchase> page = new PageImpl<>(Collections.singletonList(testPurchase), pageable, 1);

        when(purchaseRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PageResponse<PurchaseResponseDTO> result = purchaseService.getPurchases(
                1L, "PENDING", LocalDate.now().minusDays(7), LocalDate.now(), pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getSupplier().getName()).isEqualTo("TechSource Distribution Ltd");
    }

    @Test
    @DisplayName("getPurchases: Should throw IllegalArgumentException when invalid status provided")
    void getPurchases_InvalidStatus() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> purchaseService.getPurchases(null, "INVALID_STATUS", null, null, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status value");
    }

    @Test
    @DisplayName("getPurchaseById: Should return purchase order details when ID exists")
    void getPurchaseById_Success() {
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(testPurchase));

        PurchaseResponseDTO result = purchaseService.getPurchaseById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(PurchaseStatus.PENDING);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductName()).isEqualTo("Logitech Wireless Mouse M220 Silent");
        assertThat(result.getItems().get(0).getTotalCost()).isEqualTo(new BigDecimal("12400.00"));
    }

    @Test
    @DisplayName("getPurchaseById: Should throw ResourceNotFoundException when purchase not found")
    void getPurchaseById_NotFound() {
        when(purchaseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseService.getPurchaseById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Purchase order not found with ID: 999");
    }

    @Test
    @DisplayName("createPurchase: Should compute line costs, total amount, and save in PENDING status")
    void createPurchase_Success() {
        PurchaseRequestDTO request = PurchaseRequestDTO.builder()
                .supplierId(1L)
                .purchaseDate(LocalDate.now())
                .notes("Bulk restock")
                .items(Arrays.asList(
                        PurchaseItemRequestDTO.builder()
                                .productId(1L)
                                .quantity(10)
                                .unitCost(new BigDecimal("600.00"))
                                .build(),
                        PurchaseItemRequestDTO.builder()
                                .productId(2L)
                                .quantity(5)
                                .unitCost(new BigDecimal("900.00"))
                                .build()
                ))
                .build();

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(testProduct2));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> {
            Purchase saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        PurchaseResponseDTO result = purchaseService.createPurchase(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getStatus()).isEqualTo(PurchaseStatus.PENDING);
        // 10 * 600 = 6000.00, 5 * 900 = 4500.00 -> total = 10500.00
        assertThat(result.getTotalAmount()).isEqualByComparingTo("10500.00");
        assertThat(result.getItems()).hasSize(2);
        verify(purchaseRepository, times(1)).save(any(Purchase.class));
    }

    @Test
    @DisplayName("createPurchase: Should throw IllegalArgumentException when supplier is INACTIVE")
    void createPurchase_InactiveSupplier() {
        testSupplier.setStatus(SupplierStatus.INACTIVE);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));

        PurchaseRequestDTO request = PurchaseRequestDTO.builder()
                .supplierId(1L)
                .items(Collections.singletonList(
                        PurchaseItemRequestDTO.builder().productId(1L).quantity(5).unitCost(new BigDecimal("100.00")).build()
                ))
                .build();

        assertThatThrownBy(() -> purchaseService.createPurchase(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot create purchase order for inactive supplier");

        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    @DisplayName("createPurchase: Should throw ResourceNotFoundException when supplier not found")
    void createPurchase_SupplierNotFound() {
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        PurchaseRequestDTO request = PurchaseRequestDTO.builder()
                .supplierId(999L)
                .items(Collections.singletonList(
                        PurchaseItemRequestDTO.builder().productId(1L).quantity(5).unitCost(new BigDecimal("100.00")).build()
                ))
                .build();

        assertThatThrownBy(() -> purchaseService.createPurchase(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Supplier not found with ID: 999");
    }

    @Test
    @DisplayName("createPurchase: Should throw ResourceNotFoundException when product not found")
    void createPurchase_ProductNotFound() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        PurchaseRequestDTO request = PurchaseRequestDTO.builder()
                .supplierId(1L)
                .items(Collections.singletonList(
                        PurchaseItemRequestDTO.builder().productId(999L).quantity(5).unitCost(new BigDecimal("100.00")).build()
                ))
                .build();

        assertThatThrownBy(() -> purchaseService.createPurchase(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with ID: 999");
    }

    @Test
    @DisplayName("createPurchase: Should throw IllegalArgumentException when duplicate product appears in items")
    void createPurchase_DuplicateProduct() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));

        PurchaseRequestDTO request = PurchaseRequestDTO.builder()
                .supplierId(1L)
                .items(Arrays.asList(
                        PurchaseItemRequestDTO.builder().productId(1L).quantity(5).unitCost(new BigDecimal("100.00")).build(),
                        PurchaseItemRequestDTO.builder().productId(1L).quantity(10).unitCost(new BigDecimal("95.00")).build()
                ))
                .build();

        assertThatThrownBy(() -> purchaseService.createPurchase(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate product ID found in purchase line items: 1");
    }

    @Test
    @DisplayName("receivePurchase: Should ingest stock, update product purchase price, and set status to RECEIVED")
    void receivePurchase_Success() {
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(testPurchase));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(testInventory1));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseReceiveResponseDTO result = purchaseService.receivePurchase(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(PurchaseStatus.RECEIVED);
        assertThat(result.getReceivedAt()).isNotNull();

        // Check stock increment: initial 10 + item 20 = 30
        assertThat(testInventory1.getQuantityOnHand()).isEqualTo(30);
        verify(inventoryRepository, times(1)).save(testInventory1);

        // Check product purchase price updated: initial 600.00 -> item unit cost 620.00
        assertThat(testProduct1.getPurchasePrice()).isEqualByComparingTo("620.00");
        verify(productRepository, times(1)).save(testProduct1);

        assertThat(testPurchase.getStatus()).isEqualTo(PurchaseStatus.RECEIVED);
        verify(purchaseRepository, times(1)).save(testPurchase);
    }

    @Test
    @DisplayName("receivePurchase: Should throw ResourceNotFoundException when purchase not found")
    void receivePurchase_NotFound() {
        when(purchaseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseService.receivePurchase(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Purchase order not found with ID: 999");
    }

    @Test
    @DisplayName("receivePurchase: Should throw IllegalStateException when purchase is already RECEIVED")
    void receivePurchase_AlreadyReceived() {
        testPurchase.setStatus(PurchaseStatus.RECEIVED);
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(testPurchase));

        assertThatThrownBy(() -> purchaseService.receivePurchase(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot receive purchase order with status: RECEIVED");

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("receivePurchase: Should throw IllegalStateException when purchase is CANCELLED")
    void receivePurchase_AlreadyCancelled() {
        testPurchase.setStatus(PurchaseStatus.CANCELLED);
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(testPurchase));

        assertThatThrownBy(() -> purchaseService.receivePurchase(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot receive purchase order with status: CANCELLED");

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelPurchase: Should cancel unreceived PENDING purchase order successfully")
    void cancelPurchase_Success() {
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(testPurchase));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseResponseDTO result = purchaseService.cancelPurchase(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(PurchaseStatus.CANCELLED);
        assertThat(testPurchase.getStatus()).isEqualTo(PurchaseStatus.CANCELLED);
        verify(purchaseRepository, times(1)).save(testPurchase);
    }

    @Test
    @DisplayName("cancelPurchase: Should throw ResourceNotFoundException when purchase not found")
    void cancelPurchase_NotFound() {
        when(purchaseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseService.cancelPurchase(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Purchase order not found with ID: 999");
    }

    @Test
    @DisplayName("cancelPurchase: Should throw IllegalStateException when trying to cancel an already RECEIVED order")
    void cancelPurchase_AlreadyReceived() {
        testPurchase.setStatus(PurchaseStatus.RECEIVED);
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(testPurchase));

        assertThatThrownBy(() -> purchaseService.cancelPurchase(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel purchase order with status: RECEIVED");

        verify(purchaseRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelPurchase: Should throw IllegalStateException when trying to cancel an already CANCELLED order")
    void cancelPurchase_AlreadyCancelled() {
        testPurchase.setStatus(PurchaseStatus.CANCELLED);
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(testPurchase));

        assertThatThrownBy(() -> purchaseService.cancelPurchase(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel purchase order with status: CANCELLED");

        verify(purchaseRepository, never()).save(any());
    }
}
