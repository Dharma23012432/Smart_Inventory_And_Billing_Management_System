package com.smartinventory.inventoryservice.dto;

import com.smartinventory.inventoryservice.entity.enums.BillStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for confirming a customer sales bill.
 * Strictly adheres to Section 3.7.5 of API_SPECIFICATION.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillConfirmResponseDTO {

    private Long id;

    private String billNumber;

    private BillStatus status;

    private BigDecimal totalAmount;
}
