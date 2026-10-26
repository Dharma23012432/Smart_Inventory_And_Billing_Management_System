package com.smartinventory.notificationservice.event;

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
 * Event consumed when a payment is completed in payment-service.
 * Consumed from RabbitMQ queue (payment.completed.queue).
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
