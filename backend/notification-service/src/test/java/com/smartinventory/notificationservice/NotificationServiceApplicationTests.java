package com.smartinventory.notificationservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test — verifies that the Spring application context loads correctly.
 * Service-specific tests will be added in Phase 8.
 */
@org.junit.jupiter.api.Disabled("Requires running PostgreSQL database instance at localhost:5433")
@SpringBootTest
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty — validates that the Spring context starts without errors.
    }
}
