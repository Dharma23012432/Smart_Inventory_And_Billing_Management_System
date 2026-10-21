package com.smartinventory.inventoryservice.controller;

import com.smartinventory.inventoryservice.dto.ApiResponse;
import com.smartinventory.inventoryservice.dto.PageResponse;
import com.smartinventory.inventoryservice.dto.ProductRequestDTO;
import com.smartinventory.inventoryservice.dto.ProductResponseDTO;
import com.smartinventory.inventoryservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing Product management endpoints.
 * Strictly adheres to Section 3.2 of API_SPECIFICATION.md with consistent /api/v1 prefix.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    /**
     * Constructor injection for ProductService.
     *
     * @param productService Product business operations service
     */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * GET /api/v1/products
     * Retrieves a paginated and filtered list of products.
     *
     * @param search     Optional search query across name and SKU
     * @param categoryId Optional category ID filter
     * @param status     Optional status filter (e.g. ACTIVE, INACTIVE, ALL - defaults to ACTIVE)
     * @param page       Page index (0-based, default: 0)
     * @param size       Page size (default: 10)
     * @param sortBy     Sort field (default: "name")
     * @param direction  Sort direction (ASC or DESC, default: "ASC")
     * @return Paginated products wrapped in standardized ApiResponse envelope
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponseDTO>>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "ACTIVE") String status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String direction) {

        log.info("REST request to get products: search='{}', categoryId={}, status='{}', page={}, size={}",
                search, categoryId, status, page, size);

        Sort.Direction sortDirection = "DESC".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        PageResponse<ProductResponseDTO> response = productService.getProducts(search, categoryId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", response));
    }

    /**
     * GET /api/v1/products/{id}
     * Retrieves single product details by primary key.
     *
     * @param id Product primary key
     * @return Single ProductResponseDTO wrapped in standardized ApiResponse envelope
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> getProductById(@PathVariable Long id) {
        log.info("REST request to get product with ID: {}", id);
        ProductResponseDTO product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product));
    }

    /**
     * GET /api/v1/products/sku/{sku}
     * Fast product lookup by unique SKU (for POS / barcode scanning).
     *
     * @param sku Stock Keeping Unit
     * @return Single ProductResponseDTO wrapped in standardized ApiResponse envelope
     */
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> getProductBySku(@PathVariable String sku) {
        log.info("REST request to get product with SKU: '{}'", sku);
        ProductResponseDTO product = productService.getProductBySku(sku);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product));
    }

    /**
     * POST /api/v1/products
     * Creates a new product and initializes its 1:1 inventory record within the same transaction.
     *
     * @param requestDTO Validated inbound product creation payload
     * @return Created ProductResponseDTO wrapped in standardized ApiResponse envelope with HTTP 201
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponseDTO>> createProduct(
            @Valid @RequestBody ProductRequestDTO requestDTO) {

        log.info("REST request to create product with SKU: '{}', name: '{}'",
                requestDTO.getSku(), requestDTO.getName());
        ProductResponseDTO createdProduct = productService.createProduct(requestDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", createdProduct));
    }

    /**
     * PUT /api/v1/products/{id}
     * Updates an existing product's attributes.
     *
     * @param id         Product primary key
     * @param requestDTO Validated inbound product update payload
     * @return Updated ProductResponseDTO wrapped in standardized ApiResponse envelope
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO requestDTO) {

        log.info("REST request to update product with ID: {}", id);
        ProductResponseDTO updatedProduct = productService.updateProduct(id, requestDTO);

        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updatedProduct));
    }

    /**
     * DELETE /api/v1/products/{id}
     * Deletes a product or soft-deactivates it based on business rules.
     *
     * @param id Product primary key
     * @return Empty data response wrapped in standardized ApiResponse envelope
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        log.info("REST request to delete product with ID: {}", id);
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    /**
     * PATCH /api/v1/products/{id}/status
     * Explicitly transitions product lifecycle status (ACTIVE / INACTIVE).
     *
     * @param id     Product primary key
     * @param status Target status ('ACTIVE' or 'INACTIVE')
     * @return Updated ProductResponseDTO wrapped in standardized ApiResponse envelope
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> updateProductStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        log.info("REST request to update status for product ID: {} to '{}'", id, status);
        ProductResponseDTO updatedProduct = productService.updateProductStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Product status updated successfully", updatedProduct));
    }
}
