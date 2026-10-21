package com.smartinventory.payment.entity.enums;

/**
 * Payment status enumeration.
 * Matches database check constraint chk_payment_status:
 * COMPLETED, FAILED, REFUNDED
 */
public enum PaymentStatus {
    COMPLETED,
    FAILED,
    REFUNDED
}
