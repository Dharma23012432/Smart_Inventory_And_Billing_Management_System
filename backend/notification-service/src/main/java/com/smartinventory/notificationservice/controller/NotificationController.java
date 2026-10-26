package com.smartinventory.notificationservice.controller;

import com.smartinventory.notificationservice.dto.ApiResponse;
import com.smartinventory.notificationservice.dto.NotificationApprovalResponseDTO;
import com.smartinventory.notificationservice.dto.NotificationResponseDTO;
import com.smartinventory.notificationservice.dto.PageResponse;
import com.smartinventory.notificationservice.entity.enums.NotificationStatus;
import com.smartinventory.notificationservice.entity.enums.NotificationType;
import com.smartinventory.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Notification operations and approval workflow.
 * Base path: /api/v1/notifications
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Get Notifications (Audit Log)
     * GET /api/v1/notifications
     *
     * @param status optional status filter
     * @param type   optional type filter
     * @param page   page number (0-indexed, default 0)
     * @param size   page size (default 20)
     * @param sort   sort field and direction (default 'createdAt,desc')
     * @return ApiResponse containing PageResponse of NotificationResponseDTO
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponseDTO>>> getNotifications(
            @RequestParam(name = "status", required = false) NotificationStatus status,
            @RequestParam(name = "type", required = false) NotificationType type,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort) {

        log.info("Received request to get notifications: status={}, type={}, page={}, size={}", status, type, page, size);
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = (sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        PageResponse<NotificationResponseDTO> result = notificationService.getNotifications(status, type, pageable);

        return ResponseEntity.ok(ApiResponse.success(result, "Notifications retrieved successfully"));
    }

    /**
     * Get Notification by ID
     * GET /api/v1/notifications/{id}
     *
     * @param id notification ID
     * @return ApiResponse containing NotificationResponseDTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationResponseDTO>> getNotificationById(@PathVariable("id") Long id) {
        log.info("Received request to get notification by ID: {}", id);
        NotificationResponseDTO result = notificationService.getNotificationById(id);
        return ResponseEntity.ok(ApiResponse.success(result, "Notification retrieved successfully"));
    }

    /**
     * Approve Low Stock Alert (Owner Workflow)
     * PUT /api/v1/notifications/{id}/approve
     *
     * @param id notification ID
     * @return ApiResponse containing NotificationApprovalResponseDTO
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<NotificationApprovalResponseDTO>> approveNotification(@PathVariable("id") Long id) {
        log.info("Received request to approve notification ID: {}", id);
        NotificationApprovalResponseDTO result = notificationService.approveNotification(id);
        return ResponseEntity.ok(ApiResponse.success(result, "Notification approved and dispatched to supplier"));
    }

    /**
     * Retry Failed Notification
     * POST /api/v1/notifications/{id}/retry
     *
     * @param id notification ID
     * @return ApiResponse containing NotificationResponseDTO
     */
    @PostMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<NotificationResponseDTO>> retryNotification(@PathVariable("id") Long id) {
        log.info("Received request to retry notification ID: {}", id);
        NotificationResponseDTO result = notificationService.retryNotification(id);
        return ResponseEntity.ok(ApiResponse.success(result, "Notification retry attempted"));
    }

    /**
     * Get Actionable Notification Count (Badge Metric)
     * GET /api/v1/notifications/actionable-count
     *
     * @return ApiResponse containing count of actionable notifications
     */
    @GetMapping("/actionable-count")
    public ResponseEntity<ApiResponse<Long>> getActionableCount() {
        long count = notificationService.getActionableCount();
        return ResponseEntity.ok(ApiResponse.success(count, "Actionable notification count retrieved"));
    }
}
