package com.smartinventory.payment.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Event published when a payment is successfully completed.
 * Published to RabbitMQ topic exchange (inventory.exchange) with routing key (payment.completed).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long paymentId;
    private Long billId;
    private String billNumber;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private BigDecimal amountPaid;
    private String paymentMethod;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime paymentDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant eventTimestamp;
}
