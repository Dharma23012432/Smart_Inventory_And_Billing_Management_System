package com.smartinventory.inventoryservice.service;

import com.smartinventory.inventoryservice.dto.PageResponse;
import com.smartinventory.inventoryservice.dto.ProductRequestDTO;
import com.smartinventory.inventoryservice.dto.ProductResponseDTO;
import com.smartinventory.inventoryservice.entity.Category;
import com.smartinventory.inventoryservice.entity.Inventory;
import com.smartinventory.inventoryservice.entity.Product;
import com.smartinventory.inventoryservice.entity.enums.ProductStatus;
import com.smartinventory.inventoryservice.exception.DuplicateResourceException;
import com.smartinventory.inventoryservice.exception.ResourceNotFoundException;
import com.smartinventory.inventoryservice.repository.CategoryRepository;
import com.smartinventory.inventoryservice.repository.ProductRepository;
import com.smartinventory.inventoryservice.service.impl.ProductServiceImpl;
import com.smartinventory.inventoryservice.service.mapper.ProductMapper;
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
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Spy
    private ProductMapper productMapper = new ProductMapper();

    @InjectMocks
    private ProductServiceImpl productService;

    private Category testCategory;
    private Product testProduct;
    private Inventory testInventory;
    private ProductRequestDTO createRequestDTO;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic gadgets")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testProduct = Product.builder()
                .id(10L)
                .category(testCategory)
                .supplierId(1L)
                .name("Wireless Optical Mouse")
                .sku("ELEC-MOU-001")
                .description("Comfortable 2.4GHz wireless mouse")
                .unitPrice(new BigDecimal("799.00"))
                .purchasePrice(new BigDecimal("500.00"))
                .unitOfMeasure("PCS")
                .status(ProductStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testInventory = Inventory.builder()
                .id(100L)
                .product(testProduct)
                .quantityOnHand(25)
                .reorderLevel(10)
                .maxStockLevel(100)
                .version(0L)
                .lastUpdatedAt(Instant.now())
                .build();

        testProduct.setInventory(testInventory);

        createRequestDTO = ProductRequestDTO.builder()
                .categoryId(1L)
                .supplierId(1L)
                .name("Wireless Optical Mouse")
                .sku("ELEC-MOU-001")
                .description("Comfortable 2.4GHz wireless mouse")
                .unitPrice(new BigDecimal("799.00"))
                .purchasePrice(new BigDecimal("500.00"))
                .unitOfMeasure("PCS")
                .status("ACTIVE")
                .initialStock(25)
                .reorderLevel(10)
                .maxStockLevel(100)
                .build();
    }

    @Test
    @DisplayName("getProducts should return paginated response")
    void getProducts_shouldReturnPagedProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(Collections.singletonList(testProduct), pageable, 1);

        when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PageResponse<ProductResponseDTO> response = productService.getProducts("mouse", 1L, "ACTIVE", pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Wireless Optical Mouse");
        assertThat(response.getContent().get(0).getCategory().getName()).isEqualTo("Electronics");
        assertThat(response.getContent().get(0).getInventory().getQuantityOnHand()).isEqualTo(25);
        verify(productRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("getProductById when exists should return product response")
    void getProductById_whenExists_shouldReturnProduct() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));

        ProductResponseDTO response = productService.getProductById(10L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getSku()).isEqualTo("ELEC-MOU-001");
        assertThat(response.getCategory().getId()).isEqualTo(1L);
        verify(productRepository, times(1)).findById(10L);
    }

    @Test
    @DisplayName("getProductById when not found should throw ResourceNotFoundException")
    void getProductById_whenNotFound_shouldThrowException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product with id '99' was not found");
    }

    @Test
    @DisplayName("getProductBySku when exists should return product response")
    void getProductBySku_whenExists_shouldReturnProduct() {
        when(productRepository.findBySkuIgnoreCase("ELEC-MOU-001")).thenReturn(Optional.of(testProduct));

        ProductResponseDTO response = productService.getProductBySku("elec-mou-001");

        assertThat(response).isNotNull();
        assertThat(response.getSku()).isEqualTo("ELEC-MOU-001");
        verify(productRepository, times(1)).findBySkuIgnoreCase("ELEC-MOU-001");
    }

    @Test
    @DisplayName("getProductBySku when not found should throw ResourceNotFoundException")
    void getProductBySku_whenNotFound_shouldThrowException() {
        when(productRepository.findBySkuIgnoreCase("NON-EXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductBySku("NON-EXISTENT"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product with sku 'NON-EXISTENT' was not found");
    }

    @Test
    @DisplayName("createProduct success should save product and initialize inventory")
    void createProduct_success_shouldCreateProductAndInventory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.existsBySkuIgnoreCase("ELEC-MOU-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(10L);
            if (p.getInventory() != null) {
                p.getInventory().setId(100L);
            }
            return p;
        });

        ProductResponseDTO response = productService.createProduct(createRequestDTO);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getSku()).isEqualTo("ELEC-MOU-001");
        assertThat(response.getInventory().getQuantityOnHand()).isEqualTo(25);
        assertThat(response.getInventory().getReorderLevel()).isEqualTo(10);
        assertThat(response.getInventory().getMaxStockLevel()).isEqualTo(100);

        verify(categoryRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).existsBySkuIgnoreCase("ELEC-MOU-001");
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("createProduct when category does not exist should throw ResourceNotFoundException")
    void createProduct_whenCategoryNotFound_shouldThrowException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        createRequestDTO.setCategoryId(99L);

        assertThatThrownBy(() -> productService.createProduct(createRequestDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category with id '99' was not found");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("createProduct when SKU already exists should throw DuplicateResourceException")
    void createProduct_whenSkuExists_shouldThrowException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.existsBySkuIgnoreCase("ELEC-MOU-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(createRequestDTO))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Product with sku 'ELEC-MOU-001' already exists");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("createProduct when reorder level >= max stock level should throw IllegalArgumentException")
    void createProduct_whenInvalidStockLevels_shouldThrowException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.existsBySkuIgnoreCase("ELEC-MOU-001")).thenReturn(false);

        createRequestDTO.setReorderLevel(150);
        createRequestDTO.setMaxStockLevel(100);

        assertThatThrownBy(() -> productService.createProduct(createRequestDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be strictly less than maximum stock level");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("createProduct when supplierId is negative should throw IllegalArgumentException")
    void createProduct_whenNegativeSupplierId_shouldThrowException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        createRequestDTO.setSupplierId(-5L);

        assertThatThrownBy(() -> productService.createProduct(createRequestDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Supplier ID must be a positive integer");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProduct success should update product attributes and inventory settings")
    void updateProduct_success_shouldUpdate() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("ELEC-MOU-001", 10L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductRequestDTO updateRequest = ProductRequestDTO.builder()
                .categoryId(1L)
                .supplierId(1L)
                .name("Wireless Optical Mouse V2")
                .sku("ELEC-MOU-001")
                .unitPrice(new BigDecimal("849.00"))
                .purchasePrice(new BigDecimal("550.00"))
                .unitOfMeasure("PCS")
                .status("ACTIVE")
                .reorderLevel(15)
                .maxStockLevel(120)
                .build();

        ProductResponseDTO response = productService.updateProduct(10L, updateRequest);

        assertThat(response).isNotNull();
        verify(productRepository, times(1)).save(testProduct);
    }

    @Test
    @DisplayName("updateProduct when SKU in use by another product should throw DuplicateResourceException")
    void updateProduct_whenSkuInUse_shouldThrowException() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("ELEC-OTHER-SKU", 10L)).thenReturn(true);

        ProductRequestDTO updateRequest = ProductRequestDTO.builder()
                .categoryId(1L)
                .name("Updated Mouse")
                .sku("ELEC-OTHER-SKU")
                .unitPrice(new BigDecimal("849.00"))
                .purchasePrice(new BigDecimal("550.00"))
                .unitOfMeasure("PCS")
                .build();

        assertThatThrownBy(() -> productService.updateProduct(10L, updateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Product with sku 'ELEC-OTHER-SKU' already exists");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteProduct when exists should delete and flush")
    void deleteProduct_whenExists_shouldDelete() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));

        productService.deleteProduct(10L);

        verify(productRepository, times(1)).findById(10L);
        verify(productRepository, times(1)).delete(testProduct);
        verify(productRepository, times(1)).flush();
    }

    @Test
    @DisplayName("deleteProduct when not found should throw ResourceNotFoundException")
    void deleteProduct_whenNotFound_shouldThrowException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product with id '99' was not found");

        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    @DisplayName("updateProductStatus success should toggle status")
    void updateProductStatus_success() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductResponseDTO response = productService.updateProductStatus(10L, "INACTIVE");

        assertThat(response).isNotNull();
        assertThat(testProduct.getStatus()).isEqualTo(ProductStatus.INACTIVE);
        verify(productRepository, times(1)).save(testProduct);
    }

    @Test
    @DisplayName("updateProductStatus invalid status should throw IllegalArgumentException")
    void updateProductStatus_invalidStatus_shouldThrowException() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(testProduct));

        assertThatThrownBy(() -> productService.updateProductStatus(10L, "INVALID_STATUS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Status must be either ACTIVE or INACTIVE");

        verify(productRepository, never()).save(any());
    }
}
