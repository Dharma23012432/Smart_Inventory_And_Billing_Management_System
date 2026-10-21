package com.smartinventory.inventoryservice.repository;

import com.smartinventory.inventoryservice.entity.Bill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Bill entities.
 * Includes JpaSpecificationExecutor for dynamic searching, filtering, and pagination.
 * Utilizes EntityGraph to prevent N+1 queries.
 */
@Repository
public interface BillRepository extends JpaRepository<Bill, Long>, JpaSpecificationExecutor<Bill> {

    /**
     * Retrieves a bill by primary key with customer, line items, and product details eagerly loaded.
     *
     * @param id Bill primary key
     * @return Optional Bill
     */
    @Override
    @NonNull
    @EntityGraph(attributePaths = {"customer", "items", "items.product"})
    Optional<Bill> findById(@NonNull Long id);

    /**
     * Retrieves a bill by its unique human-readable invoice number with all associations eagerly loaded.
     *
     * @param billNumber Unique invoice number (e.g., INV-2026-0001)
     * @return Optional Bill
     */
    @EntityGraph(attributePaths = {"customer", "items", "items.product"})
    Optional<Bill> findByBillNumber(String billNumber);

    /**
     * Checks if a bill exists with the specified bill number.
     *
     * @param billNumber Invoice number
     * @return true if exists, false otherwise
     */
    boolean existsByBillNumber(String billNumber);

    /**
     * Checks if any bills exist for the specified customer ID.
     *
     * @param customerId Customer ID
     * @return true if bills exist, false otherwise
     */
    boolean existsByCustomerId(Long customerId);

    /**
     * Finds all bills with pagination and eagerly loaded customer details.
     *
     * @param pageable Pagination and sorting parameters
     * @return Paginated bills
     */
    @Override
    @NonNull
    @EntityGraph(attributePaths = {"customer"})
    Page<Bill> findAll(@NonNull Pageable pageable);

    /**
     * Executes dynamic specification query with pagination and eagerly loaded customer details.
     *
     * @param spec     JPA criteria specification
     * @param pageable Pagination and sorting parameters
     * @return Paginated bills
     */
    @Override
    @NonNull
    @EntityGraph(attributePaths = {"customer"})
    Page<Bill> findAll(Specification<Bill> spec, @NonNull Pageable pageable);

    /**
     * Finds the maximum bill number starting with a specific prefix for sequence generation.
     *
     * @param prefix Prefix (e.g., "INV-2026-")
     * @return Optional maximum bill number
     */
    @Query("SELECT MAX(b.billNumber) FROM Bill b WHERE b.billNumber LIKE CONCAT(:prefix, '%')")
    Optional<String> findMaxBillNumberByPrefix(@Param("prefix") String prefix);

    /**
     * Counts bills on a given date excluding a status.
     *
     * @param billDate Target date
     * @param status   Status to exclude (e.g. CANCELLED)
     * @return count of bills
     */
    long countByBillDateAndStatusNot(
            java.time.LocalDate billDate,
            com.smartinventory.inventoryservice.entity.enums.BillStatus status);

    /**
     * Finds bills on a given date excluding a status.
     *
     * @param billDate Target date
     * @param status   Status to exclude
     * @return List of bills
     */
    List<Bill> findByBillDateAndStatusNot(
            java.time.LocalDate billDate,
            com.smartinventory.inventoryservice.entity.enums.BillStatus status);

    /**
     * Finds bills in a date range excluding a status.
     *
     * @param startDate Range start
     * @param endDate   Range end
     * @param status    Status to exclude
     * @return List of bills
     */
    List<Bill> findByBillDateBetweenAndStatusNot(
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            com.smartinventory.inventoryservice.entity.enums.BillStatus status);

    /**
     * Finds recent bills with eager associations excluding a status.
     *
     * @param status   Status to exclude
     * @param pageable Pagination
     * @return Page of bills
     */
    @EntityGraph(attributePaths = {"customer", "items", "items.product"})
    Page<Bill> findByStatusNot(
            com.smartinventory.inventoryservice.entity.enums.BillStatus status,
            Pageable pageable);
}
