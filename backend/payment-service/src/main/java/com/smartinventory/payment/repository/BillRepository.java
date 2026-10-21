package com.smartinventory.payment.repository;

import com.smartinventory.payment.entity.Bill;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for {@link Bill} entity in payment service.
 */
@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    /**
     * Finds a bill by ID with its customer relation loaded.
     *
     * @param id the bill ID
     * @return Optional containing Bill if found
     */
    @EntityGraph(attributePaths = {"customer"})
    Optional<Bill> findById(Long id);
}
