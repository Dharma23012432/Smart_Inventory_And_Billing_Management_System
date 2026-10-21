package com.smartinventory.inventoryservice.repository;

import com.smartinventory.inventoryservice.entity.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for PurchaseItem entities.
 */
@Repository
public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {

    /**
     * Finds all purchase items for a given purchase order ID.
     *
     * @param purchaseId Purchase ID
     * @return List of PurchaseItems
     */
    List<PurchaseItem> findByPurchaseId(Long purchaseId);

    /**
     * Checks if any purchase items reference a given product ID.
     *
     * @param productId Product ID
     * @return true if items reference this product, false otherwise
     */
    boolean existsByProductId(Long productId);
}
