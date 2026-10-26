package com.smartinventory.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

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
        // Must run before Spring starts: the PostgreSQL JDBC driver sends the JVM's
        // default timezone on connect, and "Asia/Calcutta" is rejected by newer Postgres.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

        SpringApplication.run(PaymentServiceApplication.class, args);
        log.info("Payment Service started successfully.");
    }
}