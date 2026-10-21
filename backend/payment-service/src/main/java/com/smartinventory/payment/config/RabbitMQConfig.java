package com.smartinventory.payment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for RabbitMQ in Payment Service.
 * Defines exchange, JSON message converter, and custom RabbitTemplate.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange:inventory.exchange}")
    private String exchange;

    /**
     * Declares the topic exchange used across microservices.
     *
     * @return TopicExchange instance
     */
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchange, true, false);
    }

    /**
     * Jackson JSON message converter for serializing events.
     *
     * @param objectMapper configured Jackson ObjectMapper
     * @return MessageConverter
     */
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * RabbitTemplate configured with the JSON message converter.
     *
     * @param connectionFactory connection factory
     * @param messageConverter  json message converter
     * @return RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
