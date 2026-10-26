package com.smartinventory.notificationservice.repository;

import com.smartinventory.notificationservice.entity.Bill;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Bill entity within Notification Service.
 */
@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"customer"})
    Optional<Bill> findById(@NonNull Long id);
}
