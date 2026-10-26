package com.smartinventory.notificationservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * Entry point for the Notification Service.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Consuming {@code PaymentCompleted} and {@code LowStockDetected} events from RabbitMQ</li>
 *   <li>Invoice PDF generation</li>
 *   <li>Email dispatch via SMTP</li>
 *   <li>Notification persistence</li>
 *   <li>Supplier approval workflow</li>
 * </ul>
 *
 * <p>Runs on port {@code 8082}.
 */
@SpringBootApplication
public class NotificationServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceApplication.class);

    public static void main(String[] args) {
        // Must run before Spring starts: the PostgreSQL JDBC driver sends the JVM's
        // default timezone on connect, and "Asia/Calcutta" is rejected by newer Postgres.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

        SpringApplication.run(NotificationServiceApplication.class, args);
        log.info("Notification Service started successfully.");
    }
}