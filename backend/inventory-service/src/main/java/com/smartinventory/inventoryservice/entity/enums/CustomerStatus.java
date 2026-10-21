package com.smartinventory.inventoryservice.entity.enums;

/**
 * Lifecycle status for customers.
 * Matches chk_customer_status constraint in DATABASE_SPECIFICATION.md.
 */
public enum CustomerStatus {
    ACTIVE,
    INACTIVE
}
