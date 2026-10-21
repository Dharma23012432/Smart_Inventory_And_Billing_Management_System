package com.smartinventory.inventoryservice.service;

import com.smartinventory.inventoryservice.dto.BillConfirmResponseDTO;
import com.smartinventory.inventoryservice.dto.BillRequestDTO;
import com.smartinventory.inventoryservice.dto.BillResponseDTO;
import com.smartinventory.inventoryservice.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * Service interface defining sales billing and invoicing operations.
 * Strictly adheres to Section 3.7 of API_SPECIFICATION.md.
 */
public interface BillService {

    /**
     * Retrieves paginated and filtered sales bills.
     *
     * @param customerId  Optional customer ID filter
     * @param status      Optional bill status filter (DRAFT, CONFIRMED, PAID, CANCELLED)
     * @param startDate   Optional lower bound bill date
     * @param endDate     Optional upper bound bill date
     * @param invoiceSent Optional email dispatch audit filter
     * @param pageable    Pagination and sorting parameters
     * @return Paginated bills
     */
    PageResponse<BillResponseDTO> getBills(
            Long customerId,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            Boolean invoiceSent,
            Pageable pageable
    );

    /**
     * Retrieves a full bill breakdown by primary key.
     *
     * @param id Bill primary key
     * @return BillResponseDTO
     */
    BillResponseDTO getBillById(Long id);

    /**
     * Retrieves a bill by its customer-facing invoice number.
     *
     * @param billNumber Invoice identifier (e.g., INV-2026-0001)
     * @return BillResponseDTO
     */
    BillResponseDTO getBillByBillNumber(String billNumber);

    /**
     * Initializes a customer sales bill with line items in DRAFT status.
     *
     * @param requestDTO Inbound bill payload
     * @return Created BillResponseDTO
     */
    BillResponseDTO createBill(BillRequestDTO requestDTO);

    /**
     * Confirms a DRAFT bill, verifies stock availability, deducts inventory,
     * and evaluates low-stock notification triggers.
     *
     * @param id Bill primary key
     * @return BillConfirmResponseDTO
     */
    BillConfirmResponseDTO confirmBill(Long id);

    /**
     * Cancels a bill. Restocks inventory if previously CONFIRMED.
     *
     * @param id Bill primary key
     * @return Updated BillResponseDTO
     */
    BillResponseDTO cancelBill(Long id);
}
