package com.smartinventory.notificationservice.repository;

import com.smartinventory.notificationservice.entity.Notification;
import com.smartinventory.notificationservice.entity.enums.NotificationStatus;
import com.smartinventory.notificationservice.entity.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for {@link Notification} entity.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    /**
     * Finds notifications by status and type with pagination.
     */
    Page<Notification> findByStatusAndType(NotificationStatus status, NotificationType type, Pageable pageable);

    /**
     * Finds notifications by status with pagination.
     */
    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);

    /**
     * Finds notifications by type with pagination.
     */
    Page<Notification> findByType(NotificationType type, Pageable pageable);

    /**
     * Checks if a pending notification already exists for the given reference ID and type.
     */
    boolean existsByReferenceIdAndTypeAndStatus(Long referenceId, NotificationType type, NotificationStatus status);

    /**
     * Finds the latest notification by reference type, reference ID, and notification type.
     */
    Optional<Notification> findFirstByReferenceTypeAndReferenceIdAndTypeOrderByCreatedAtDesc(
            String referenceType, Long referenceId, NotificationType type);

    /**
     * Finds notifications by reference type, reference ID, and notification status.
     */
    List<Notification> findByReferenceTypeAndReferenceIdAndStatus(String referenceType, Long referenceId, NotificationStatus status);

    /**
     * Counts actionable notifications:
     * 1. Low stock alerts pending owner approval
     * 2. Failed emails requiring administrative attention / retry
     */
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(n) FROM Notification n WHERE (n.type = com.smartinventory.notificationservice.entity.enums.NotificationType.LOW_STOCK_ALERT AND n.status = com.smartinventory.notificationservice.entity.enums.NotificationStatus.PENDING) OR n.status = com.smartinventory.notificationservice.entity.enums.NotificationStatus.FAILED")
    long countActionableNotifications();
}
