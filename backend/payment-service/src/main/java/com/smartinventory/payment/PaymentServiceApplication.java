package com.smartinventory.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Payment Service.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Payment processing</li>
 *   <li>Payment validation</li>
 *   <li>Payment persistence</li>
 *   <li>Publishing {@code PaymentCompleted} events to RabbitMQ</li>
 * </ul>
 *
 * <p>Runs on port {@code 8081}.
 */
@SpringBootApplication
public class PaymentServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
        log.info("Payment Service started successfully.");
    }
}
