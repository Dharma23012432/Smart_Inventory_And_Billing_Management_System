package com.smartinventory.inventoryservice.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;

/**
 * Production implementation of EventPublisher using Spring AMQP RabbitTemplate.
 * Publishes events to inventory.exchange with designated routing keys.
 */
@Slf4j
@Component
public class RabbitEventPublisher implements EventPublisher {

    public static final String EXCHANGE_NAME = "inventory.exchange";
    public static final String LOW_STOCK_ROUTING_KEY = "stock.low";

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

            amqpTemplate.convertAndSend(EXCHANGE_NAME, LOW_STOCK_ROUTING_KEY, event);
        } catch (Exception ex) {
            log.error("Failed to publish LowStockDetected event to RabbitMQ: {}", ex.getMessage(), ex);
        }
    }
}
