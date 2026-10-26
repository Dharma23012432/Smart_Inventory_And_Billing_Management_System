package com.smartinventory.notificationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ broker configuration for the Notification Service.
 * Declares exchanges, queues, bindings, and JSON message converter.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange:inventory.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.queue.bill-confirmed:bill.confirmed.queue}")
    private String billConfirmedQueueName;

    @Value("${app.rabbitmq.queue.payment-completed:payment.completed.queue}")
    private String paymentCompletedQueueName;

    @Value("${app.rabbitmq.queue.low-stock:stock.low.queue}")
    private String lowStockQueueName;

    @Value("${app.rabbitmq.queue.bill-cancelled:bill.cancelled.queue}")
    private String billCancelledQueueName;

    @Value("${app.rabbitmq.routing-key.bill-confirmed:bill.confirmed}")
    private String billConfirmedRoutingKey;

    @Value("${app.rabbitmq.routing-key.payment-completed:payment.completed}")
    private String paymentCompletedRoutingKey;

    @Value("${app.rabbitmq.routing-key.low-stock:stock.low}")
    private String lowStockRoutingKey;

    @Value("${app.rabbitmq.routing-key.bill-cancelled:bill.cancelled}")
    private String billCancelledRoutingKey;

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue billConfirmedQueue() {
        return QueueBuilder.durable(billConfirmedQueueName).build();
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return QueueBuilder.durable(paymentCompletedQueueName).build();
    }

    @Bean
    public Queue lowStockQueue() {
        return QueueBuilder.durable(lowStockQueueName).build();
    }

    @Bean
    public Queue billCancelledQueue() {
        return QueueBuilder.durable(billCancelledQueueName).build();
    }

    @Bean
    public Binding billConfirmedBinding(Queue billConfirmedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(billConfirmedQueue).to(exchange).with(billConfirmedRoutingKey);
    }

    @Bean
    public Binding paymentCompletedBinding(Queue paymentCompletedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(paymentCompletedQueue).to(exchange).with(paymentCompletedRoutingKey);
    }

    @Bean
    public Binding lowStockBinding(Queue lowStockQueue, TopicExchange exchange) {
        return BindingBuilder.bind(lowStockQueue).to(exchange).with(lowStockRoutingKey);
    }

    @Bean
    public Binding billCancelledBinding(Queue billCancelledQueue, TopicExchange exchange) {
        return BindingBuilder.bind(billCancelledQueue).to(exchange).with(billCancelledRoutingKey);
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
