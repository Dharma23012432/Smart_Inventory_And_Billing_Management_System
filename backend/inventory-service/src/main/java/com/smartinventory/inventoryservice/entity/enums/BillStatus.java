package com.smartinventory.inventoryservice.entity.enums;

/**
 * Status enumeration for Customer Sales Bills.
 * Strictly adheres to check constraint chk_bill_status in schema.sql.
 */
public enum BillStatus {
    DRAFT,
    CONFIRMED,
    PAID,
    CANCELLED
}
