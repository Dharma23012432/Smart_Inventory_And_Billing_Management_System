package com.smartinventory.notificationservice.event;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event wrapper for PaymentCompleted event broadcast over RabbitMQ.
 */
public class PaymentCompletedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private PaymentCompletedPayload payload;

    public PaymentCompletedEvent() {
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
