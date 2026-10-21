package com.smartinventory.inventoryservice.repository;

import com.smartinventory.inventoryservice.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Customer entities.
 * Includes JpaSpecificationExecutor for dynamic searching, filtering, and pagination.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    /**
     * Finds customer by exact phone number.
     *
     * @param phone Customer contact number
     * @return Optional Customer
     */
    Optional<Customer> findByPhone(String phone);

    /**
     * Retrieves all customers sorted alphabetically by name.
     *
     * @return List of customers
     */
    List<Customer> findAllByOrderByNameAsc();

    /**
     * Counts customers with given status.
     *
     * @param status CustomerStatus
     * @return count
     */
    long countByStatus(com.smartinventory.inventoryservice.entity.enums.CustomerStatus status);
}
