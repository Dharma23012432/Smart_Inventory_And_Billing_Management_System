package com.smartinventory.payment.entity.enums;

/**
 * Bill status enumeration.
 * Matches database check constraint chk_bill_status:
 * DRAFT, CONFIRMED, PAID, CANCELLED
 */
public enum BillStatus {
    DRAFT,
    CONFIRMED,
    PAID,
    CANCELLED
}
