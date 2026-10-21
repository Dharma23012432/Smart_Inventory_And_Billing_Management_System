package com.smartinventory.payment.entity.enums;

/**
 * Payment method enumeration.
 * Matches database check constraint chk_payment_method:
 * CASH, CARD, UPI, BANK_TRANSFER, CHEQUE
 */
public enum PaymentMethod {
    CASH,
    CARD,
    UPI,
    BANK_TRANSFER,
    CHEQUE
}
