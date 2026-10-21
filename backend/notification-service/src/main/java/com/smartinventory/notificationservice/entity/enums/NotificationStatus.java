package com.smartinventory.notificationservice.entity.enums;

/**
 * Notification lifecycle status enumeration.
 * Matches database check constraint chk_notification_status:
 * PENDING, APPROVED, SENT, FAILED
 */
public enum NotificationStatus {
    PENDING,
    APPROVED,
    SENT,
    FAILED
}
