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

    /**
     * Publishes a BillConfirmed event to the message broker.
     *
     * @param event The BillConfirmedEvent payload
     */
    void publishBillConfirmedEvent(BillConfirmedEvent event);

    /**
     * Publishes a BillCancelled event to the message broker.
     *
     * @param event The BillCancelledEvent payload
     */
    void publishBillCancelledEvent(BillCancelledEvent event);
}
