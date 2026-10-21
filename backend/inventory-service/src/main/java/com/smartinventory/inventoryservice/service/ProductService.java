package com.smartinventory.inventoryservice.service;

import com.smartinventory.inventoryservice.dto.PageResponse;
import com.smartinventory.inventoryservice.dto.ProductRequestDTO;
import com.smartinventory.inventoryservice.dto.ProductResponseDTO;
import org.springframework.data.domain.Pageable;

/**
 * Service interface defining business operations for Product management.
 * Strictly adheres to Section 3.2 of API_SPECIFICATION.md and DATABASE_SPECIFICATION.md.
 */
public interface ProductService {

    /**
     * Retrieves a paginated list of products based on optional search and filter criteria.
     * Eagerly loads Category and Inventory details to prevent N+1 queries.
     *
     * @param search     Optional text query to match product name or SKU (case-insensitive)
     * @param categoryId Optional category ID filter
     * @param status     Optional status filter (e.g. ACTIVE, INACTIVE, ALL)
     * @param pageable   Pagination and sorting parameters
     * @return Standardized PageResponse of ProductResponseDTOs
     */
    PageResponse<ProductResponseDTO> getProducts(String search, Long categoryId, String status, Pageable pageable);

    /**
     * Retrieves a single product by its primary key.
     *
     * @param id Product primary key
     * @return ProductResponseDTO with category and inventory details
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if product not found
     */
    ProductResponseDTO getProductById(Long id);

    /**
     * Retrieves a single product by its unique Stock Keeping Unit (SKU).
     *
     * @param sku Stock Keeping Unit
     * @return ProductResponseDTO
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if product not found
     */
    ProductResponseDTO getProductBySku(String sku);

    /**
     * Creates a new product and automatically initializes its 1:1 inventory record within the same transaction.
     *
     * @param requestDTO Inbound product creation payload
     * @return ProductResponseDTO
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if category does not exist
     * @throws com.smartinventory.inventoryservice.exception.DuplicateResourceException if SKU already exists
     * @throws IllegalArgumentException if pricing or inventory levels violate business constraints
     */
    ProductResponseDTO createProduct(ProductRequestDTO requestDTO);

    /**
     * Updates an existing product's attributes.
     *
     * @param id         Product primary key
     * @param requestDTO Inbound product update payload
     * @return ProductResponseDTO
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if product or category not found
     * @throws com.smartinventory.inventoryservice.exception.DuplicateResourceException if SKU belongs to another product
     * @throws IllegalArgumentException if pricing or inventory levels violate business constraints
     */
    ProductResponseDTO updateProduct(Long id, ProductRequestDTO requestDTO);

    /**
     * Deletes or soft-deactivates a product depending on transaction history.
     * If historical purchase or bill items exist, sets status to INACTIVE.
     * If no transactional history exists, purges product and associated inventory record.
     *
     * @param id Product primary key
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if product not found
     */
    void deleteProduct(Long id);

    /**
     * Explicitly updates a product's lifecycle status (ACTIVE or INACTIVE).
     *
     * @param id     Product primary key
     * @param status Target status ('ACTIVE' or 'INACTIVE')
     * @return Updated ProductResponseDTO
     * @throws com.smartinventory.inventoryservice.exception.ResourceNotFoundException if product not found
     * @throws IllegalArgumentException if status string is invalid
     */
    ProductResponseDTO updateProductStatus(Long id, String status);
}
