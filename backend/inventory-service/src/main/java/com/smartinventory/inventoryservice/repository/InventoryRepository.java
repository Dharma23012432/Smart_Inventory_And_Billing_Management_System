package com.smartinventory.inventoryservice.repository;

import com.smartinventory.inventoryservice.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Inventory entities.
 * Includes JpaSpecificationExecutor and eager EntityGraphs to prevent N+1 queries.
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {

    /**
     * Finds inventory record by associated Product ID with eagerly fetched product details.
     *
     * @param productId Associated product ID
     * @return Optional Inventory record
     */
    @EntityGraph(attributePaths = {"product"})
    Optional<Inventory> findByProductId(Long productId);

    /**
     * Checks if inventory record exists for product ID.
     *
     * @param productId Associated product ID
     * @return true if exists, false otherwise
     */
    boolean existsByProductId(Long productId);

    /**
     * Finds inventory by primary key with eagerly fetched product details.
     *
     * @param id Inventory primary key
     * @return Optional Inventory record
     */
    @Override
    @NonNull
    @EntityGraph(attributePaths = {"product"})
    Optional<Inventory> findById(@NonNull Long id);

    /**
     * Finds all inventory records with pagination and eager product graph.
     *
     * @param pageable Pagination and sorting parameters
     * @return Paginated inventory
     */
    @Override
    @NonNull
    @EntityGraph(attributePaths = {"product"})
    Page<Inventory> findAll(@NonNull Pageable pageable);

    /**
     * Executes dynamic specification queries with eager product graph and pagination.
     *
     * @param spec     JPA criteria specification
     * @param pageable Pagination and sorting parameters
     * @return Paginated inventory
     */
    @Override
    @NonNull
    @EntityGraph(attributePaths = {"product"})
    Page<Inventory> findAll(Specification<Inventory> spec, @NonNull Pageable pageable);

    /**
     * Retrieves all inventory items where current quantity on hand is less than or equal to reorder level.
     *
     * @param pageable Pagination and sorting parameters
     * @return Paginated low-stock inventory records
     */
    @EntityGraph(attributePaths = {"product"})
    @Query("SELECT i FROM Inventory i WHERE i.quantityOnHand <= i.reorderLevel")
    Page<Inventory> findLowStock(Pageable pageable);

    /**
     * Retrieves all low-stock items across the entire catalog.
     *
     * @return List of all low stock inventory records
     */
    @EntityGraph(attributePaths = {"product"})
    @Query("SELECT i FROM Inventory i WHERE i.quantityOnHand <= i.reorderLevel")
    List<Inventory> findAllLowStock();

    /**
     * Counts low-stock items where quantity on hand is less than or equal to reorder level.
     *
     * @return count of low stock items
     */
    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.quantityOnHand <= i.reorderLevel")
    long countLowStock();
}
