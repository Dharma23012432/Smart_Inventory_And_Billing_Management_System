package com.smartinventory.inventoryservice.repository;

import com.smartinventory.inventoryservice.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Supplier entities.
 * Includes JpaSpecificationExecutor for dynamic searching, filtering, and pagination.
 */
@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long>, JpaSpecificationExecutor<Supplier> {

    /**
     * Finds supplier by email address (case-insensitive).
     *
     * @param email Supplier email address
     * @return Optional Supplier
     */
    Optional<Supplier> findByEmailIgnoreCase(String email);

    /**
     * Checks if a supplier with the given email exists (case-insensitive).
     *
     * @param email Supplier email
     * @return true if email exists, false otherwise
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Checks if another supplier already uses the given email, excluding a specific ID.
     *
     * @param email Supplier email
     * @param id    Supplier ID to exclude
     * @return true if email exists on another supplier, false otherwise
     */
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /**
     * Retrieves all suppliers sorted alphabetically by name.
     *
     * @return List of suppliers
     */
    List<Supplier> findAllByOrderByNameAsc();
}
