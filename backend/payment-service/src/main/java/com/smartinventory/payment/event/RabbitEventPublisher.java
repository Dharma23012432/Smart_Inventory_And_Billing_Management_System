package com.smartinventory.payment.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ implementation of {@link EventPublisher}.
 * Publishes events to the configured RabbitMQ topic exchange.
 * Failures to publish are logged safely without failing the core transactional payment processing.
 */
@Component
public class RabbitEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String paymentCompletedRoutingKey;

    /**
     * Constructor injection for RabbitEventPublisher.
     *
     * @param rabbitTemplate              the Spring AMQP RabbitTemplate
     * @param exchange                     the target exchange name
     * @param paymentCompletedRoutingKey the routing key for payment completed events
     */
    public RabbitEventPublisher(RabbitTemplate rabbitTemplate,
                                @Value("${app.rabbitmq.exchange:inventory.exchange}") String exchange,
                                @Value("${app.rabbitmq.routing-key.payment-completed:payment.completed}") String paymentCompletedRoutingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.paymentCompletedRoutingKey = paymentCompletedRoutingKey;
    }

    @Override
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        try {
            log.info("Publishing PaymentCompletedEvent to exchange: '{}' with routing key: '{}' for paymentId: {}",
                    exchange, paymentCompletedRoutingKey, event.getPaymentId());
            rabbitTemplate.convertAndSend(exchange, paymentCompletedRoutingKey, event);
            log.info("Successfully published PaymentCompletedEvent for paymentId: {}", event.getPaymentId());
        } catch (Exception ex) {
            log.error("Failed to publish PaymentCompletedEvent for paymentId: {}. Error: {}",
                    event.getPaymentId(), ex.getMessage(), ex);
            // Non-blocking: We don't rollback the payment transaction if the message broker is temporarily down
        }
    }
}
