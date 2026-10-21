package com.smartinventory.notificationservice.event;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event wrapper for LowStockDetected event broadcast over RabbitMQ.
 */
public class LowStockDetectedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private LowStockDetectedPayload payload;

    public LowStockDetectedEvent() {
    }

    public LowStockDetectedEvent(String eventId, String eventType, LocalDateTime timestamp, LowStockDetectedPayload payload) {
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

    public LowStockDetectedPayload getPayload() {
        return payload;
    }

    public void setPayload(LowStockDetectedPayload payload) {
        this.payload = payload;
    }
}
