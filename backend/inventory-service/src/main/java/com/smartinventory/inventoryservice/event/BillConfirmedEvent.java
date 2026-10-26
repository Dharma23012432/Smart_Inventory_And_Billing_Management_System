package com.smartinventory.inventoryservice.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Event published when a sales bill is successfully confirmed.
 * Contains only the required fields per workflow specifications.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillConfirmedEvent implements Serializable {

    private Long billId;
    private String billNumber;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private BigDecimal totalAmount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate billDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant eventTimestamp;
}
