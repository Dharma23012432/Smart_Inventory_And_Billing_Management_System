package com.smartinventory.inventoryservice.event;

/**
 * Interface defining event publishing operations for asynchronous messaging.
 */
public interface EventPublisher {

    /**
     * Publishes a LowStockDetected event to the message broker.
     *
     * @param event The LowStockEvent payload
     */
    void publishLowStockEvent(LowStockEvent event);
}
