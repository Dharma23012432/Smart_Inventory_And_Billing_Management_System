package com.smartinventory.payment.repository;

import com.smartinventory.payment.entity.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for {@link Payment} entity.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Checks if a payment already exists for the given bill ID.
     *
     * @param billId the bill ID
     * @return true if a payment exists, false otherwise
     */
    boolean existsByBillId(Long billId);

    /**
     * Finds payment record by bill ID, eagerly fetching associated Bill and Customer.
     *
     * @param billId the bill ID
     * @return Optional containing the Payment if found
     */
    @EntityGraph(attributePaths = {"bill", "bill.customer"})
    Optional<Payment> findByBillId(Long billId);

    /**
     * Finds payment record by payment ID with Bill and Customer eagerly fetched.
     *
     * @param id payment ID
     * @return Optional containing Payment
     */
    @Override
    @EntityGraph(attributePaths = {"bill", "bill.customer"})
    Optional<Payment> findById(Long id);
}
