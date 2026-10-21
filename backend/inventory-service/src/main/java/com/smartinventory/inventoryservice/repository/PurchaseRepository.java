package com.smartinventory.inventoryservice.repository;

import com.smartinventory.inventoryservice.entity.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Purchase entities.
 * Includes JpaSpecificationExecutor for dynamic searching, filtering, and pagination.
 * Utilizes EntityGraph to prevent N+1 queries.
 */
@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long>, JpaSpecificationExecutor<Purchase> {

    /**
     * Retrieves a purchase order by ID with supplier, line items, and product details eagerly loaded.
     *
     * @param id Purchase order primary key
     * @return Optional Purchase with full details
     */
    @Override
    @NonNull
    @EntityGraph(attributePaths = {"supplier", "items", "items.product"})
    Optional<Purchase> findById(@NonNull Long id);

    /**
     * Finds all purchase orders with pagination and eagerly fetched supplier details.
     *
     * @param pageable Pagination and sorting parameters
     * @return Paginated purchase orders
     */
    @Override
    @NonNull
    @EntityGraph(attributePaths = {"supplier"})
    Page<Purchase> findAll(@NonNull Pageable pageable);

    /**
     * Executes dynamic criteria specification with pagination and eagerly fetched supplier details.
     *
     * @param spec     Dynamic JPA criteria specification
     * @param pageable Pagination and sorting parameters
     * @return Paginated purchase orders
     */
    @Override
    @NonNull
    @EntityGraph(attributePaths = {"supplier"})
    Page<Purchase> findAll(Specification<Purchase> spec, @NonNull Pageable pageable);

    /**
     * Checks if any purchase orders reference a given supplier ID.
     *
     * @param supplierId Supplier primary key
     * @return true if purchase orders exist for this supplier, false otherwise
     */
    boolean existsBySupplierId(Long supplierId);

    /**
     * Counts purchase orders with given status.
     *
     * @param status PurchaseStatus
     * @return count
     */
    long countByStatus(com.smartinventory.inventoryservice.entity.enums.PurchaseStatus status);

    /**
     * Finds purchase orders in date range excluding a status.
     *
     * @param startDate Range start
     * @param endDate   Range end
     * @param status    Status to exclude (e.g. CANCELLED)
     * @return List of purchases
     */
    List<Purchase> findByPurchaseDateBetweenAndStatusNot(
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            com.smartinventory.inventoryservice.entity.enums.PurchaseStatus status);

    /**
     * Finds purchase orders for a supplier in date range excluding a status.
     *
     * @param supplierId Supplier ID
     * @param startDate  Range start
     * @param endDate    Range end
     * @param status     Status to exclude
     * @return List of purchases
     */
    List<Purchase> findBySupplierIdAndPurchaseDateBetweenAndStatusNot(
            Long supplierId,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            com.smartinventory.inventoryservice.entity.enums.PurchaseStatus status);
}
