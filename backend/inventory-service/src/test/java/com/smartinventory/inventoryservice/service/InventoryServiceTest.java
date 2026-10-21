package com.smartinventory.inventoryservice.service;

import com.smartinventory.inventoryservice.dto.InventoryAdjustmentRequestDTO;
import com.smartinventory.inventoryservice.dto.InventoryResponseDTO;
import com.smartinventory.inventoryservice.dto.InventorySettingsRequestDTO;
import com.smartinventory.inventoryservice.dto.PageResponse;
import com.smartinventory.inventoryservice.entity.Category;
import com.smartinventory.inventoryservice.entity.Inventory;
import com.smartinventory.inventoryservice.entity.Product;
import com.smartinventory.inventoryservice.entity.enums.ProductStatus;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.repository.InventoryRepository;
import com.smartinventory.inventoryservice.service.impl.InventoryServiceImpl;
import com.smartinventory.inventoryservice.service.mapper.InventoryMapper;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Unit Tests")
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Spy
    private InventoryMapper inventoryMapper = new InventoryMapper();

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Product testProduct;
    private Inventory testInventory;

    @BeforeEach
    void setUp() {
        Category category = Category.builder()
                .id(1L)
                .name("Electronics")
                .build();

        testProduct = Product.builder()
                .id(10L)
                .category(category)
                .name("Wireless Optical Mouse")
                .sku("ELEC-MOU-001")
                .unitPrice(new BigDecimal("799.00"))
                .purchasePrice(new BigDecimal("500.00"))
                .unitOfMeasure("PCS")
                .status(ProductStatus.ACTIVE)
                .build();

        testInventory = Inventory.builder()
                .id(100L)
                .product(testProduct)
                .quantityOnHand(8)
                .reorderLevel(10)
                .maxStockLevel(100)
                .version(0L)
                .lastUpdatedAt(Instant.now())
                .build();

        testProduct.setInventory(testInventory);
    }

    @Test
    @DisplayName("getInventoryList when lowStockOnly is false should return all inventory items")
    void getInventoryList_all_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Inventory> page = new PageImpl<>(Collections.singletonList(testInventory), pageable, 1);

        when(inventoryRepository.findAll(pageable)).thenReturn(page);

        PageResponse<InventoryResponseDTO> response = inventoryService.getInventoryList(false, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getQuantityOnHand()).isEqualTo(8);
        assertThat(response.getContent().get(0).getProductName()).isEqualTo("Wireless Optical Mouse");
        assertThat(response.getContent().get(0).getIsLowStock()).isTrue();

        verify(inventoryRepository, times(1)).findAll(pageable);
        verify(inventoryRepository, never()).findLowStock(any());
    }

    @Test
    @DisplayName("getInventoryList when lowStockOnly is true should return low stock items")
    void getInventoryList_lowStockOnly_shouldReturnFilteredPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Inventory> page = new PageImpl<>(Collections.singletonList(testInventory), pageable, 1);

        when(inventoryRepository.findLowStock(pageable)).thenReturn(page);

        PageResponse<InventoryResponseDTO> response = inventoryService.getInventoryList(true, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getIsLowStock()).isTrue();

        verify(inventoryRepository, times(1)).findLowStock(pageable);
        verify(inventoryRepository, never()).findAll(pageable);
    }

    @Test
    @DisplayName("getInventoryById when exists should return InventoryResponseDTO")
    void getInventoryById_whenExists_shouldReturn() {
        when(inventoryRepository.findById(100L)).thenReturn(Optional.of(testInventory));

        InventoryResponseDTO response = inventoryService.getInventoryById(100L);

        assertThat(response).isNotNull();
        assertThat(response.getInventoryId()).isEqualTo(100L);
        assertThat(response.getProductId()).isEqualTo(10L);
        assertThat(response.getSku()).isEqualTo("ELEC-MOU-001");
        verify(inventoryRepository, times(1)).findById(100L);
    }

    @Test
    @DisplayName("getInventoryById when not found should throw ResourceNotFoundException")
    void getInventoryById_whenNotFound_shouldThrow() {
        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getInventoryById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Inventory with id '999' was not found");
    }

    @Test
    @DisplayName("getInventoryByProductId when exists should return InventoryResponseDTO")
    void getInventoryByProductId_whenExists_shouldReturn() {
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(testInventory));

        InventoryResponseDTO response = inventoryService.getInventoryByProductId(10L);

        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isEqualTo(10L);
        assertThat(response.getQuantityOnHand()).isEqualTo(8);
        verify(inventoryRepository, times(1)).findByProductId(10L);
    }

    @Test
    @DisplayName("getInventoryByProductId when not found should throw ResourceNotFoundException")
    void getInventoryByProductId_whenNotFound_shouldThrow() {
        when(inventoryRepository.findByProductId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getInventoryByProductId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Inventory with productId '99' was not found");
    }

    @Test
    @DisplayName("getLowStockInventory should call findLowStock repository method")
    void getLowStockInventory_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Inventory> page = new PageImpl<>(Collections.singletonList(testInventory), pageable, 1);

        when(inventoryRepository.findLowStock(pageable)).thenReturn(page);

        PageResponse<InventoryResponseDTO> response = inventoryService.getLowStockInventory(pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        verify(inventoryRepository, times(1)).findLowStock(pageable);
    }

    @Test
    @DisplayName("adjustStock success should update quantity and timestamp")
    void adjustStock_success() {
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        InventoryAdjustmentRequestDTO request = InventoryAdjustmentRequestDTO.builder()
                .adjustedQuantity(45)
                .reason("Quarterly audit recount")
                .build();

        InventoryResponseDTO response = inventoryService.adjustStock(10L, request);

        assertThat(response).isNotNull();
        assertThat(testInventory.getQuantityOnHand()).isEqualTo(45);
        assertThat(testInventory.isLowStock()).isFalse();
        verify(inventoryRepository, times(1)).save(testInventory);
    }

    @Test
    @DisplayName("adjustStock with negative quantity should throw IllegalArgumentException")
    void adjustStock_negativeQuantity_shouldThrow() {
        InventoryAdjustmentRequestDTO request = InventoryAdjustmentRequestDTO.builder()
                .adjustedQuantity(-10)
                .reason("Damaged stock")
                .build();

        assertThatThrownBy(() -> inventoryService.adjustStock(10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock quantity cannot be negative");

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateReorderSettings success should update reorderLevel and maxStockLevel")
    void updateReorderSettings_success() {
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        InventorySettingsRequestDTO request = InventorySettingsRequestDTO.builder()
                .reorderLevel(15)
                .maxStockLevel(150)
                .build();

        InventoryResponseDTO response = inventoryService.updateReorderSettings(10L, request);

        assertThat(response).isNotNull();
        assertThat(testInventory.getReorderLevel()).isEqualTo(15);
        assertThat(testInventory.getMaxStockLevel()).isEqualTo(150);
        verify(inventoryRepository, times(1)).save(testInventory);
    }

    @Test
    @DisplayName("updateReorderSettings when reorderLevel >= maxStockLevel should throw IllegalArgumentException")
    void updateReorderSettings_invalidThresholds_shouldThrow() {
        InventorySettingsRequestDTO request = InventorySettingsRequestDTO.builder()
                .reorderLevel(100)
                .maxStockLevel(50)
                .build();

        assertThatThrownBy(() -> inventoryService.updateReorderSettings(10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be strictly less than maximum stock level");

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateReorderLevel success should update reorderLevel")
    void updateReorderLevel_success() {
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        InventoryResponseDTO response = inventoryService.updateReorderLevel(10L, 20);

        assertThat(response).isNotNull();
        assertThat(testInventory.getReorderLevel()).isEqualTo(20);
        verify(inventoryRepository, times(1)).save(testInventory);
    }

    @Test
    @DisplayName("updateReorderLevel when exceeding maxStockLevel should throw IllegalArgumentException")
    void updateReorderLevel_exceedsMax_shouldThrow() {
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(testInventory));

        assertThatThrownBy(() -> inventoryService.updateReorderLevel(10L, 120))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be strictly less than maximum stock level");

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateMaxStockLevel success should update maxStockLevel")
    void updateMaxStockLevel_success() {
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        InventoryResponseDTO response = inventoryService.updateMaxStockLevel(10L, 200);

        assertThat(response).isNotNull();
        assertThat(testInventory.getMaxStockLevel()).isEqualTo(200);
        verify(inventoryRepository, times(1)).save(testInventory);
    }

    @Test
    @DisplayName("updateMaxStockLevel when below reorderLevel should throw IllegalArgumentException")
    void updateMaxStockLevel_belowReorder_shouldThrow() {
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(testInventory));

        assertThatThrownBy(() -> inventoryService.updateMaxStockLevel(10L, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be strictly less than maximum stock level");

        verify(inventoryRepository, never()).save(any());
    }
}
