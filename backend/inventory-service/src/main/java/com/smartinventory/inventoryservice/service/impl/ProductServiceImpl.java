package com.smartinventory.inventoryservice.service.impl;

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
import com.smartinventory.inventoryservice.service.ProductService;
import com.smartinventory.inventoryservice.service.mapper.ProductMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Production implementation of ProductService.
 * Follows layered architecture, constructor injection, SOLID principles, and SLF4J logging.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private static final int DEFAULT_REORDER_LEVEL = 10;
    private static final int DEFAULT_MAX_STOCK_LEVEL = 100;
    private static final int DEFAULT_INITIAL_STOCK = 0;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    /**
     * Constructor injection for required dependencies.
     *
     * @param productRepository  Repository for Product entities
     * @param categoryRepository Repository for Category entities
     * @param productMapper      Mapper for Product entity-DTO conversion
     */
    public ProductServiceImpl(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productMapper = productMapper;
    }

    @Override
    public PageResponse<ProductResponseDTO> getProducts(
            String search, Long categoryId, String status, Pageable pageable) {

        log.info("Fetching products page with search='{}', categoryId={}, status='{}', pageable={}",
                search, categoryId, status, pageable);

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Status filter: defaults to ACTIVE if null/empty, matches status if provided, skips if "ALL"
            if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
                try {
                    ProductStatus productStatus = ProductStatus.valueOf(status.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), productStatus));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status filter '{}' ignored", status);
                }
            } else if (status == null) {
                predicates.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));
            }

            // Category filter
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // Search query across name and SKU (case-insensitive)
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate namePredicate = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate skuPredicate = cb.like(cb.lower(root.get("sku")), searchPattern);
                predicates.add(cb.or(namePredicate, skuPredicate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> page = productRepository.findAll(spec, pageable);
        log.debug("Retrieved {} products across {} total pages", page.getNumberOfElements(), page.getTotalPages());

        return PageResponse.from(page, productMapper::toResponseDTO);
    }

    @Override
    public ProductResponseDTO getProductById(Long id) {
        log.info("Fetching product by ID: {}", id);
        Product product = findProductOrThrow(id);
        return productMapper.toResponseDTO(product);
    }

    @Override
    public ProductResponseDTO getProductBySku(String sku) {
        String trimmedSku = sku.trim().toUpperCase();
        log.info("Fetching product by SKU: '{}'", trimmedSku);

        Product product = productRepository.findBySkuIgnoreCase(trimmedSku)
                .orElseThrow(() -> {
                    log.warn("Product with SKU '{}' not found", trimmedSku);
                    return new ResourceNotFoundException("Product", "sku", trimmedSku);
                });

        return productMapper.toResponseDTO(product);
    }

    @Override
    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO requestDTO) {
        String trimmedSku = requestDTO.getSku().trim().toUpperCase();
        log.info("Creating product with SKU: '{}', name: '{}'", trimmedSku, requestDTO.getName());

        // 1. Verify Category existence
        Category category = categoryRepository.findById(requestDTO.getCategoryId())
                .orElseThrow(() -> {
                    log.warn("Category with ID {} does not exist", requestDTO.getCategoryId());
                    return new ResourceNotFoundException("Category", "id", requestDTO.getCategoryId());
                });

        // 2. Validate supplier ID if provided
        validateSupplierIfExists(requestDTO.getSupplierId());

        // 3. Verify SKU uniqueness
        if (productRepository.existsBySkuIgnoreCase(trimmedSku)) {
            log.warn("Product creation rejected — SKU '{}' already exists", trimmedSku);
            throw new DuplicateResourceException("Product", "sku", trimmedSku);
        }

        // 4. Validate reorder level and max stock level constraints
        int reorderLevel = requestDTO.getReorderLevel() != null ? requestDTO.getReorderLevel() : DEFAULT_REORDER_LEVEL;
        int maxStockLevel = requestDTO.getMaxStockLevel() != null ? requestDTO.getMaxStockLevel() : DEFAULT_MAX_STOCK_LEVEL;
        if (reorderLevel >= maxStockLevel) {
            throw new IllegalArgumentException(String.format(
                    "Reorder level (%d) must be strictly less than maximum stock level (%d)",
                    reorderLevel, maxStockLevel));
        }

        // 5. Convert DTO to Product entity
        Product product = productMapper.toEntity(requestDTO, category);

        // 6. Automatically initialize 1:1 Inventory record within the same transaction
        int initialStock = requestDTO.getInitialStock() != null ? requestDTO.getInitialStock() : DEFAULT_INITIAL_STOCK;
        Inventory inventory = Inventory.builder()
                .product(product)
                .quantityOnHand(initialStock)
                .reorderLevel(reorderLevel)
                .maxStockLevel(maxStockLevel)
                .version(0L)
                .lastUpdatedAt(Instant.now())
                .build();

        product.setInventory(inventory);

        // 7. Persist product with cascaded inventory
        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {}, SKU: '{}'", savedProduct.getId(), savedProduct.getSku());

        return productMapper.toResponseDTO(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO requestDTO) {
        String trimmedSku = requestDTO.getSku().trim().toUpperCase();
        log.info("Updating product ID: {}, target SKU: '{}'", id, trimmedSku);

        // 1. Find existing Product
        Product product = findProductOrThrow(id);

        // 2. Verify Category existence
        Category category = categoryRepository.findById(requestDTO.getCategoryId())
                .orElseThrow(() -> {
                    log.warn("Category with ID {} does not exist", requestDTO.getCategoryId());
                    return new ResourceNotFoundException("Category", "id", requestDTO.getCategoryId());
                });

        // 3. Validate supplier ID if provided
        validateSupplierIfExists(requestDTO.getSupplierId());

        // 4. Verify SKU uniqueness excluding current product ID
        if (productRepository.existsBySkuIgnoreCaseAndIdNot(trimmedSku, id)) {
            log.warn("Product update rejected — SKU '{}' already in use by another product", trimmedSku);
            throw new DuplicateResourceException("Product", "sku", trimmedSku);
        }

        // 5. Validate and update reorder & max stock thresholds on associated inventory
        if (product.getInventory() != null) {
            int reorder = requestDTO.getReorderLevel() != null
                    ? requestDTO.getReorderLevel()
                    : product.getInventory().getReorderLevel();
            int maxStock = requestDTO.getMaxStockLevel() != null
                    ? requestDTO.getMaxStockLevel()
                    : product.getInventory().getMaxStockLevel();

            if (reorder >= maxStock) {
                throw new IllegalArgumentException(String.format(
                        "Reorder level (%d) must be strictly less than maximum stock level (%d)",
                        reorder, maxStock));
            }

            if (requestDTO.getReorderLevel() != null) {
                product.getInventory().setReorderLevel(requestDTO.getReorderLevel());
            }
            if (requestDTO.getMaxStockLevel() != null) {
                product.getInventory().setMaxStockLevel(requestDTO.getMaxStockLevel());
            }
            product.getInventory().setLastUpdatedAt(Instant.now());
        }

        // 6. Update Product attributes
        productMapper.updateEntityFromDTO(requestDTO, product, category);

        Product updatedProduct = productRepository.save(product);
        log.info("Product with ID: {} updated successfully", id);

        return productMapper.toResponseDTO(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Processing delete request for product ID: {}", id);
        Product product = findProductOrThrow(id);

        productRepository.delete(product);
        productRepository.flush();
        log.info("Product with ID: {} purged successfully", id);
    }

    @Override
    @Transactional
    public ProductResponseDTO updateProductStatus(Long id, String status) {
        log.info("Updating status for product ID: {} to '{}'", id, status);
        Product product = findProductOrThrow(id);

        try {
            ProductStatus productStatus = ProductStatus.valueOf(status.trim().toUpperCase());
            product.setStatus(productStatus);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid product status '{}' provided", status);
            throw new IllegalArgumentException("Status must be either ACTIVE or INACTIVE");
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Product ID: {} status updated to {}", id, product.getStatus());

        return productMapper.toResponseDTO(updatedProduct);
    }

    // -----------------------------------------------------------------
    // Internal Helper Methods
    // -----------------------------------------------------------------

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product with ID: {} not found", id);
                    return new ResourceNotFoundException("Product", "id", id);
                });
    }

    private void validateSupplierIfExists(Long supplierId) {
        if (supplierId != null && supplierId <= 0) {
            log.warn("Invalid supplier ID {} rejected", supplierId);
            throw new IllegalArgumentException("Supplier ID must be a positive integer");
        }
    }
}
