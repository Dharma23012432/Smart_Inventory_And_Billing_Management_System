package com.smartinventory.payment.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event wrapper for PaymentCompleted event broadcast over RabbitMQ.
 * Matches API_SPECIFICATION.md Section 7.1.
 */
public class PaymentCompletedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private PaymentCompletedPayload payload;

    public PaymentCompletedEvent() {
    }

    public PaymentCompletedEvent(PaymentCompletedPayload payload) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = "PaymentCompleted";
        this.timestamp = LocalDateTime.now();
        this.payload = payload;
    }

    public PaymentCompletedEvent(String eventId, String eventType, LocalDateTime timestamp, PaymentCompletedPayload payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public PaymentCompletedPayload getPayload() {
        return payload;
    }

    public void setPayload(PaymentCompletedPayload payload) {
        this.payload = payload;
    }
}
