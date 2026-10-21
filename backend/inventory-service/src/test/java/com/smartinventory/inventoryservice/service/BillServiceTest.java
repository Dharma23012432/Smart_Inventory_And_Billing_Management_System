package com.smartinventory.inventoryservice.service;

import com.smartinventory.inventoryservice.dto.*;
import com.smartinventory.inventoryservice.entity.*;
import com.smartinventory.inventoryservice.entity.enums.BillStatus;
import com.smartinventory.inventoryservice.entity.enums.CustomerStatus;
import com.smartinventory.inventoryservice.event.EventPublisher;
import com.smartinventory.inventoryservice.event.LowStockEvent;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.repository.*;
import com.smartinventory.inventoryservice.service.impl.BillServiceImpl;
import com.smartinventory.inventoryservice.service.mapper.BillMapper;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillService Unit Tests")
class BillServiceTest {

    @Mock
    private BillRepository billRepository;

    @Mock
    private BillItemRepository billItemRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Spy
    private BillMapper billMapper = new BillMapper();

    @InjectMocks
    private BillServiceImpl billService;

    private Customer testCustomer;
    private Product testProduct;
    private Inventory testInventory;
    private Bill testBill;
    private BillItem testBillItem;

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
                .id(2L)
                .name("Priya Sharma")
                .email("priya.sharma@example.com")
                .phone("+91 98765 22222")
                .status(CustomerStatus.ACTIVE)
                .build();

        testProduct = Product.builder()
                .id(1L)
                .name("Logitech Wireless Mouse M220 Silent")
                .sku("ELEC-LOG-M220")
                .unitPrice(new BigDecimal("899.00"))
                .purchasePrice(new BigDecimal("600.00"))
                .supplierId(1L)
                .unitOfMeasure("PIECE")
                .build();

        testInventory = Inventory.builder()
                .id(101L)
                .product(testProduct)
                .quantityOnHand(10)
                .reorderLevel(5)
                .maxStockLevel(100)
                .version(0L)
                .lastUpdatedAt(Instant.now())
                .build();

        testBill = Bill.builder()
                .id(101L)
                .billNumber("INV-2026-0001")
                .customer(testCustomer)
                .billDate(LocalDate.now())
                .subtotal(new BigDecimal("1798.00"))
                .discountAmount(new BigDecimal("100.00"))
                .taxAmount(new BigDecimal("305.64"))
                .totalAmount(new BigDecimal("2003.64"))
                .status(BillStatus.DRAFT)
                .invoiceSent(false)
                .notes("Counter sale")
                .items(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testBillItem = BillItem.builder()
                .id(201L)
                .bill(testBill)
                .product(testProduct)
                .quantity(2)
                .unitPrice(new BigDecimal("899.00"))
                .discountPercent(BigDecimal.ZERO)
                .totalPrice(new BigDecimal("1798.00"))
                .build();

        testBill.addItem(testBillItem);
    }

    @Test
    @DisplayName("getBills: Should return paginated bills matching criteria")
    void getBills_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Bill> page = new PageImpl<>(Collections.singletonList(testBill), pageable, 1);

        when(billRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PageResponse<BillResponseDTO> result = billService.getBills(
                2L, "DRAFT", LocalDate.now().minusDays(5), LocalDate.now(), false, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getBillNumber()).isEqualTo("INV-2026-0001");
        assertThat(result.getContent().get(0).getCustomer().getName()).isEqualTo("Priya Sharma");
    }

    @Test
    @DisplayName("getBills: Should throw IllegalArgumentException on invalid status")
    void getBills_InvalidStatus() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> billService.getBills(null, "INVALID_STATUS", null, null, null, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status value");
    }

    @Test
    @DisplayName("getBillById: Should return full bill details when ID exists")
    void getBillById_Success() {
        when(billRepository.findById(101L)).thenReturn(Optional.of(testBill));

        BillResponseDTO result = billService.getBillById(101L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(101L);
        assertThat(result.getBillNumber()).isEqualTo("INV-2026-0001");
        assertThat(result.getStatus()).isEqualTo(BillStatus.DRAFT);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductName()).isEqualTo("Logitech Wireless Mouse M220 Silent");
    }

    @Test
    @DisplayName("getBillById: Should throw ResourceNotFoundException when bill not found")
    void getBillById_NotFound() {
        when(billRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> billService.getBillById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Bill not found with ID: 999");
    }

    @Test
    @DisplayName("getBillByBillNumber: Should return bill when billNumber matches")
    void getBillByBillNumber_Success() {
        when(billRepository.findByBillNumber("INV-2026-0001")).thenReturn(Optional.of(testBill));

        BillResponseDTO result = billService.getBillByBillNumber("INV-2026-0001");

        assertThat(result).isNotNull();
        assertThat(result.getBillNumber()).isEqualTo("INV-2026-0001");
    }

    @Test
    @DisplayName("createBill: Should generate sequential bill number, calculate totals, and save in DRAFT")
    void createBill_Success() {
        BillRequestDTO request = BillRequestDTO.builder()
                .customerId(2L)
                .discountAmount(new BigDecimal("100.00"))
                .taxPercent(new BigDecimal("18.00"))
                .notes("Counter sale")
                .items(Collections.singletonList(
                        BillItemRequestDTO.builder()
                                .productId(1L)
                                .quantity(2)
                                .discountPercent(BigDecimal.ZERO)
                                .build()
                ))
                .build();

        int currentYear = LocalDate.now().getYear();
        String prefix = String.format("INV-%d-", currentYear);
        when(billRepository.findMaxBillNumberByPrefix(prefix)).thenReturn(Optional.of(prefix + "0005"));

        when(customerRepository.findById(2L)).thenReturn(Optional.of(testCustomer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> {
            Bill saved = invocation.getArgument(0);
            saved.setId(102L);
            return saved;
        });

        BillResponseDTO result = billService.createBill(request);

        assertThat(result).isNotNull();
        assertThat(result.getBillNumber()).isEqualTo(prefix + "0006");
        assertThat(result.getStatus()).isEqualTo(BillStatus.DRAFT);
        // 2 * 899.00 = 1798.00 subtotal, discount 100.00 -> taxable 1698.00, tax 18% = 305.64 -> total = 2003.64
        assertThat(result.getSubtotal()).isEqualByComparingTo("1798.00");
        assertThat(result.getDiscountAmount()).isEqualByComparingTo("100.00");
        assertThat(result.getTaxAmount()).isEqualByComparingTo("305.64");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("2003.64");

        verify(billRepository, times(1)).save(any(Bill.class));
    }

    @Test
    @DisplayName("createBill: Should throw IllegalArgumentException when customer is INACTIVE")
    void createBill_InactiveCustomer() {
        testCustomer.setStatus(CustomerStatus.INACTIVE);
        when(customerRepository.findById(2L)).thenReturn(Optional.of(testCustomer));

        BillRequestDTO request = BillRequestDTO.builder()
                .customerId(2L)
                .items(Collections.singletonList(
                        BillItemRequestDTO.builder().productId(1L).quantity(1).build()
                ))
                .build();

        assertThatThrownBy(() -> billService.createBill(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot create bill for inactive customer");

        verify(billRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBill: Should throw IllegalArgumentException when duplicate product appears in items")
    void createBill_DuplicateProduct() {
        when(customerRepository.findById(2L)).thenReturn(Optional.of(testCustomer));

        BillRequestDTO request = BillRequestDTO.builder()
                .customerId(2L)
                .items(Arrays.asList(
                        BillItemRequestDTO.builder().productId(1L).quantity(1).build(),
                        BillItemRequestDTO.builder().productId(1L).quantity(2).build()
                ))
                .build();

        assertThatThrownBy(() -> billService.createBill(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate product ID found in bill line items: 1");
    }

    @Test
    @DisplayName("confirmBill: Should deduct inventory, update status to CONFIRMED, and return summary")
    void confirmBill_Success() {
        when(billRepository.findById(101L)).thenReturn(Optional.of(testBill));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(testInventory));
        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Initial inventory quantity = 10, item quantity = 2 -> remaining = 8 (> reorderLevel 5)
        BillConfirmResponseDTO result = billService.confirmBill(101L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(101L);
        assertThat(result.getStatus()).isEqualTo(BillStatus.CONFIRMED);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("2003.64");

        assertThat(testInventory.getQuantityOnHand()).isEqualTo(8);
        verify(inventoryRepository, times(1)).save(testInventory);
        verify(billRepository, times(1)).save(testBill);
        verify(eventPublisher, never()).publishLowStockEvent(any());
    }

    @Test
    @DisplayName("confirmBill: Should trigger LowStockDetected event when stock drops to or below reorder level")
    void confirmBill_LowStockEventTriggered() {
        // Initial inventory = 6, requested = 2 -> remaining = 4 (<= reorderLevel 5)
        testInventory.setQuantityOnHand(6);
        testInventory.setReorderLevel(5);

        Supplier supplier = Supplier.builder()
                .id(1L)
                .name("TechSource Distribution Ltd")
                .email("orders@techsource.com")
                .build();

        when(billRepository.findById(101L)).thenReturn(Optional.of(testBill));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(testInventory));
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BillConfirmResponseDTO result = billService.confirmBill(101L);

        assertThat(result).isNotNull();
        assertThat(testInventory.getQuantityOnHand()).isEqualTo(4);
        verify(eventPublisher, times(1)).publishLowStockEvent(any(LowStockEvent.class));
    }

    @Test
    @DisplayName("confirmBill: Should throw IllegalStateException when stock is insufficient")
    void confirmBill_InsufficientStock() {
        // Initial inventory = 1, requested = 2
        testInventory.setQuantityOnHand(1);

        when(billRepository.findById(101L)).thenReturn(Optional.of(testBill));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(testInventory));

        assertThatThrownBy(() -> billService.confirmBill(101L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock for product");

        // Verify stock was not deducted and bill was not updated
        assertThat(testInventory.getQuantityOnHand()).isEqualTo(1);
        verify(billRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmBill: Should throw IllegalStateException when bill is not in DRAFT status")
    void confirmBill_NotDraft() {
        testBill.setStatus(BillStatus.CONFIRMED);
        when(billRepository.findById(101L)).thenReturn(Optional.of(testBill));

        assertThatThrownBy(() -> billService.confirmBill(101L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot confirm bill with status: CONFIRMED");

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelBill: Should cancel DRAFT bill without touching inventory")
    void cancelBill_FromDraft() {
        when(billRepository.findById(101L)).thenReturn(Optional.of(testBill));
        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BillResponseDTO result = billService.cancelBill(101L);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(BillStatus.CANCELLED);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelBill: Should restock inventory when cancelling a CONFIRMED bill")
    void cancelBill_FromConfirmed() {
        testBill.setStatus(BillStatus.CONFIRMED);
        testInventory.setQuantityOnHand(8);

        when(billRepository.findById(101L)).thenReturn(Optional.of(testBill));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(testInventory));
        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BillResponseDTO result = billService.cancelBill(101L);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(BillStatus.CANCELLED);
        // 8 on hand + 2 from bill item = 10 restocked
        assertThat(testInventory.getQuantityOnHand()).isEqualTo(10);
        verify(inventoryRepository, times(1)).save(testInventory);
        verify(billRepository, times(1)).save(testBill);
    }

    @Test
    @DisplayName("cancelBill: Should throw IllegalStateException when trying to cancel a PAID bill")
    void cancelBill_AlreadyPaid() {
        testBill.setStatus(BillStatus.PAID);
        when(billRepository.findById(101L)).thenReturn(Optional.of(testBill));

        assertThatThrownBy(() -> billService.cancelBill(101L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Bills in PAID status cannot be cancelled in V1");

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelBill: Should throw IllegalStateException when bill is already CANCELLED")
    void cancelBill_AlreadyCancelled() {
        testBill.setStatus(BillStatus.CANCELLED);
        when(billRepository.findById(101L)).thenReturn(Optional.of(testBill));

        assertThatThrownBy(() -> billService.cancelBill(101L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Bill is already CANCELLED");

        verify(inventoryRepository, never()).save(any());
    }
}
