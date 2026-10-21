package com.smartinventory.payment.event;

/**
 * Interface defining contract for publishing domain events.
 */
public interface EventPublisher {

    /**
     * Publishes a PaymentCompletedEvent to the message broker.
     *
     * @param event the event to publish
     */
    void publishPaymentCompleted(PaymentCompletedEvent event);
}
