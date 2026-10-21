package com.smartinventory.inventoryservice.repository;

import com.smartinventory.inventoryservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Product entities.
 * Includes JpaSpecificationExecutor for dynamic searching, filtering, and pagination.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /**
     * Finds a product by its unique Stock Keeping Unit (case-insensitive).
     * Eagerly loads category and inventory associations using EntityGraph to eliminate N+1 queries.
     *
     * @param sku Stock Keeping Unit
     * @return Optional Product
     */
    @EntityGraph(attributePaths = {"category", "inventory"})
    Optional<Product> findBySkuIgnoreCase(String sku);

    /**
     * Checks if a product exists with the specified SKU (case-insensitive).
     *
     * @param sku Stock Keeping Unit
     * @return true if SKU exists, false otherwise
     */
    boolean existsBySkuIgnoreCase(String sku);

    /**
     * Checks if another product already uses this SKU, excluding a specific ID.
     * Used for update uniqueness verification.
     *
     * @param sku Stock Keeping Unit
     * @param id  Product ID to exclude
     * @return true if another product has this SKU, false otherwise
     */
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    /**
     * Checks if any products reference a given category ID.
     *
     * @param categoryId Category ID
     * @return true if products exist in category, false otherwise
     */
    boolean existsByCategoryId(Long categoryId);

    /**
     * Retrieves a single product by ID with category and inventory eagerly loaded.
     *
     * @param id Product primary key
     * @return Optional Product
     */
    @Override
    @NonNull
    @EntityGraph(attributePaths = {"category", "inventory"})
    Optional<Product> findById(@NonNull Long id);

    /**
     * Executes dynamic search and filter query with pagination and eager entity graphs.
     *
     * @param spec     Dynamic JPA criteria specification
     * @param pageable Pagination and sorting parameters
     * @return Paginated products
     */
    @Override
    @NonNull
    @EntityGraph(attributePaths = {"category", "inventory"})
    Page<Product> findAll(Specification<Product> spec, @NonNull Pageable pageable);

    /**
     * Counts products with given status.
     *
     * @param status ProductStatus
     * @return count
     */
    long countByStatus(com.smartinventory.inventoryservice.entity.enums.ProductStatus status);
}
