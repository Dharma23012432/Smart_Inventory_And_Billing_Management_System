package com.smartinventory.inventoryservice.dto;

import com.smartinventory.inventoryservice.entity.enums.BillStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO representing full Bill details.
 * Strictly adheres to Section 3.7.2 of API_SPECIFICATION.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillResponseDTO {

    private Long id;

    private String billNumber;

    private CustomerSummaryDTO customer;

    private LocalDate billDate;

    private BigDecimal subtotal;

    private BigDecimal discountAmount;

    private BigDecimal taxAmount;

    private BigDecimal totalAmount;

    private BillStatus status;

    private Boolean invoiceSent;

    private String notes;

    private List<BillItemResponseDTO> items;

    private Instant createdAt;

    private Instant updatedAt;
}
