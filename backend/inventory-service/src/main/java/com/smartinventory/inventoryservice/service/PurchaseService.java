package com.smartinventory.inventoryservice.service;

import com.smartinventory.inventoryservice.dto.PageResponse;
import com.smartinventory.inventoryservice.dto.PurchaseReceiveResponseDTO;
import com.smartinventory.inventoryservice.dto.PurchaseRequestDTO;
import com.smartinventory.inventoryservice.dto.PurchaseResponseDTO;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * Service interface defining procurement purchase order business operations.
 * Strictly adheres to Section 3.6 of API_SPECIFICATION.md.
 */
public interface PurchaseService {

    /**
     * Retrieves paginated and filtered purchase orders.
     *
     * @param supplierId Optional supplier ID filter
     * @param status     Optional status filter (PENDING, RECEIVED, CANCELLED)
     * @param startDate  Optional lower bound purchase date
     * @param endDate    Optional upper bound purchase date
     * @param pageable   Pagination and sorting parameters
     * @return Paginated purchase orders
     */
    PageResponse<PurchaseResponseDTO> getPurchases(
            Long supplierId,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    /**
     * Retrieves full purchase order details by ID including supplier and line items.
     *
     * @param id Purchase order primary key
     * @return PurchaseResponseDTO
     */
    PurchaseResponseDTO getPurchaseById(Long id);

    /**
     * Creates a new purchase order in PENDING status.
     * Validates active supplier, product existence, line calculations, and duplicate products.
     *
     * @param requestDTO Inbound purchase order payload
     * @return Created PurchaseResponseDTO
     */
    PurchaseResponseDTO createPurchase(PurchaseRequestDTO requestDTO);

    /**
     * Ingests stock for a PENDING purchase order.
     * Increments inventory on hand, updates product purchase price, and sets status to RECEIVED.
     *
     * @param id Purchase order primary key
     * @return PurchaseReceiveResponseDTO
     */
    PurchaseReceiveResponseDTO receivePurchase(Long id);

    /**
     * Cancels an unreceived PENDING purchase order.
     *
     * @param id Purchase order primary key
     * @return Updated PurchaseResponseDTO with CANCELLED status
     */
    PurchaseResponseDTO cancelPurchase(Long id);
}
