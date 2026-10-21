package com.smartinventory.inventoryservice.repository;

import com.smartinventory.inventoryservice.entity.BillItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for BillItem entities.
 */
@Repository
public interface BillItemRepository extends JpaRepository<BillItem, Long> {

    /**
     * Finds all bill items for a given bill ID.
     *
     * @param billId Bill primary key
     * @return List of BillItem entities
     */
    List<BillItem> findByBillId(Long billId);

    /**
     * Checks if any bill items reference a given product ID.
     *
     * @param productId Product ID
     * @return true if items reference this product, false otherwise
     */
    boolean existsByProductId(Long productId);
}
