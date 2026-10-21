package com.smartinventory.inventoryservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test — verifies that the Spring application context loads correctly.
 * Requires running PostgreSQL database instance; integration tests run in containerized environment.
 */
@Disabled("Requires running PostgreSQL database instance at localhost:5432")
@SpringBootTest
class InventoryServiceApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty — validates that the Spring context starts without errors.
    }
}
