package com.smartinventory.notificationservice.service;

import com.smartinventory.notificationservice.dto.NotificationApprovalResponseDTO;
import com.smartinventory.notificationservice.dto.NotificationResponseDTO;
import com.smartinventory.notificationservice.dto.PageResponse;
import com.smartinventory.notificationservice.entity.enums.NotificationStatus;
import com.smartinventory.notificationservice.entity.enums.NotificationType;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing and dispatching notifications.
 */
public interface NotificationService {

    /**
     * Retrieves paginated audit logs of notifications with optional status and type filters.
     *
     * @param status   optional filter by NotificationStatus
     * @param type     optional filter by NotificationType
     * @param pageable pagination parameters
     * @return PageResponse containing NotificationResponseDTO
     */
    PageResponse<NotificationResponseDTO> getNotifications(NotificationStatus status, NotificationType type, Pageable pageable);

    /**
     * Retrieves a notification by its primary key ID.
     *
     * @param id notification ID
     * @return NotificationResponseDTO
     */
    NotificationResponseDTO getNotificationById(Long id);

    /**
     * Approves a pending low-stock alert notification and dispatches an email to the supplier.
     * Sets status = APPROVED, triggers email dispatch, and marks status = SENT (or FAILED if mail fails).
     *
     * @param id notification ID
     * @return NotificationApprovalResponseDTO
     */
    NotificationApprovalResponseDTO approveNotification(Long id);

    /**
     * Re-attempts email dispatch for a failed notification.
     *
     * @param id notification ID
     * @return NotificationResponseDTO
     */
    NotificationResponseDTO retryNotification(Long id);
}
