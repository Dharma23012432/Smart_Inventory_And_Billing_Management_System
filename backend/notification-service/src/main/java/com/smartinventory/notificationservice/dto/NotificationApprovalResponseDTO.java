package com.smartinventory.notificationservice.dto;

import com.smartinventory.notificationservice.entity.enums.NotificationStatus;

import java.time.LocalDateTime;

/**
 * Data Transfer Object returned upon approving a low-stock alert.
 * Matches API_SPECIFICATION.md Section 3.9.3:
 * <pre>
 * {
 *   "id": 12,
 *   "status": "SENT",
 *   "recipientEmail": "orders@techsource.com",
 *   "sentAt": "2026-10-19T10:40:00Z"
 * }
 * </pre>
 */
public class NotificationApprovalResponseDTO {

    private Long id;
    private NotificationStatus status;
    private String recipientEmail;
    private LocalDateTime sentAt;

    public NotificationApprovalResponseDTO() {
    }

    public NotificationApprovalResponseDTO(Long id, NotificationStatus status, String recipientEmail, LocalDateTime sentAt) {
        this.id = id;
        this.status = status;
        this.recipientEmail = recipientEmail;
        this.sentAt = sentAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
