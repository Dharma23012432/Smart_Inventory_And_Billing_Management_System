package com.smartinventory.inventoryservice.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Production implementation of EventPublisher using Spring AMQP RabbitTemplate.
 * Publishes events to inventory.exchange with designated routing keys.
 */
@Slf4j
@Component
public class RabbitEventPublisher implements EventPublisher {

    @Value("${app.rabbitmq.exchange:inventory.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.routing-key.bill-confirmed:bill.confirmed}")
    private String billConfirmedRoutingKey;

    @Value("${app.rabbitmq.routing-key.low-stock:stock.low}")
    private String lowStockRoutingKey;

    @Value("${app.rabbitmq.routing-key.bill-cancelled:bill.cancelled}")
    private String billCancelledRoutingKey;

    private final AmqpTemplate amqpTemplate;

    public RabbitEventPublisher(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }

    @Override
    public void publishLowStockEvent(LowStockEvent event) {
        try {
            log.info("Publishing LowStockDetected event {} for product: {} (SKU: {})",
                    event.getEventId(),
                    event.getPayload().getProductName(),
                    event.getPayload().getSku());

            amqpTemplate.convertAndSend(exchangeName, lowStockRoutingKey, event);
        } catch (Exception ex) {
            log.error("Failed to publish LowStockDetected event to RabbitMQ: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public void publishBillConfirmedEvent(BillConfirmedEvent event) {
        try {
            log.info("Publishing BillConfirmed event for bill #{}: {}", event.getBillNumber(), event);
            amqpTemplate.convertAndSend(exchangeName, billConfirmedRoutingKey, event);
        } catch (Exception ex) {
            log.error("Failed to publish BillConfirmed event to RabbitMQ for bill #{}: {}", event.getBillNumber(), ex.getMessage(), ex);
        }
    }

    @Override
    public void publishBillCancelledEvent(BillCancelledEvent event) {
        try {
            log.info("Publishing BillCancelled event for bill #{}: {}", event.getBillNumber(), event);
            amqpTemplate.convertAndSend(exchangeName, billCancelledRoutingKey, event);
        } catch (Exception ex) {
            log.error("Failed to publish BillCancelled event to RabbitMQ for bill #{}: {}", event.getBillNumber(), ex.getMessage(), ex);
        }
    }
}
