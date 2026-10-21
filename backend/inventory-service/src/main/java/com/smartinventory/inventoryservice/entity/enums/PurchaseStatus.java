package com.smartinventory.inventoryservice.entity.enums;

/**
 * Status enumeration for Purchase orders.
 * Strictly adheres to check constraint chk_purchase_status in schema.sql.
 */
public enum PurchaseStatus {
    PENDING,
    RECEIVED,
    CANCELLED
}
