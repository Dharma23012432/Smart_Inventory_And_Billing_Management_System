package com.smartinventory.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Domain event consumed when an invoice/bill is cancelled.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillCancelledEvent implements Serializable {

    private Long billId;

    private String billNumber;

    private String reason;

    private Instant eventTimestamp;
}
