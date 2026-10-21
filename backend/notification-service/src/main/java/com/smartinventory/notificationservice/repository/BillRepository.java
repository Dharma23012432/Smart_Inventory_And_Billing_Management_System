package com.smartinventory.notificationservice.repository;

import com.smartinventory.notificationservice.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for updating Bill entity within Notification Service.
 */
@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
}
