package com.smartinventory.inventoryservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard uniform RabbitMQ event envelope for LowStockDetected.
 * Strictly adheres to Section 7.2 of API_SPECIFICATION.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockEvent {

    @Builder.Default
    private String eventId = "evt-" + UUID.randomUUID();

    @Builder.Default
    private String eventType = "LowStockDetected";

    @Builder.Default
    private Instant timestamp = Instant.now();

    private LowStockPayload payload;

    /**
     * Factory method creating a populated LowStockEvent.
     *
     * @param payload Low-stock item payload
     * @return Formatted LowStockEvent
     */
    public static LowStockEvent of(LowStockPayload payload) {
        return LowStockEvent.builder()
                .eventId("evt-" + UUID.randomUUID())
                .eventType("LowStockDetected")
                .timestamp(Instant.now())
                .payload(payload)
                .build();
    }
}
