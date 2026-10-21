package com.smartinventory.inventoryservice.dto;

import com.smartinventory.inventoryservice.entity.enums.PurchaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for receiving a purchase order.
 * Strictly adheres to Section 3.6.4 of API_SPECIFICATION.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseReceiveResponseDTO {

    private Long id;

    private PurchaseStatus status;

    private Instant receivedAt;
}
