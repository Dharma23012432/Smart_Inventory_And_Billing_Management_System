package com.smartinventory.notificationservice.entity.enums;

/**
 * Notification type enumeration.
 * Matches database check constraint chk_notification_type:
 * INVOICE_EMAIL, LOW_STOCK_ALERT
 */
public enum NotificationType {
    INVOICE_EMAIL,
    LOW_STOCK_ALERT
}
