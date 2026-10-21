package com.smartinventory.inventoryservice.dto;

import com.smartinventory.inventoryservice.entity.enums.PurchaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO representing full Purchase Order details.
 * Strictly adheres to Section 3.6.2 of API_SPECIFICATION.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseResponseDTO {

    private Long id;

    private SupplierSummaryDTO supplier;

    private LocalDate purchaseDate;

    private BigDecimal totalAmount;

    private PurchaseStatus status;

    private String notes;

    private List<PurchaseItemResponseDTO> items;

    private Instant createdAt;

    private Instant updatedAt;
}
