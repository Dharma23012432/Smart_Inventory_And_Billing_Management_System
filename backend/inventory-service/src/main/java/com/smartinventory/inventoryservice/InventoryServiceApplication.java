package com.smartinventory.inventoryservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Inventory & Billing Service.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Product management</li>
 *   <li>Customer management</li>
 *   <li>Supplier management</li>
 *   <li>Purchase management</li>
 *   <li>Inventory operations</li>
 *   <li>Billing operations</li>
 *   <li>Report generation</li>
 *   <li>Publishing {@code LowStockDetected} events to RabbitMQ</li>
 * </ul>
 *
 * <p>Runs on port {@code 8080}.
 */
@SpringBootApplication
public class InventoryServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
        log.info("Inventory & Billing Service started successfully.");
    }
}
