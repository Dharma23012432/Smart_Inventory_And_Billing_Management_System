package com.smartinventory.notificationservice.service.impl;

import com.smartinventory.notificationservice.dto.NotificationApprovalResponseDTO;
import com.smartinventory.notificationservice.dto.NotificationResponseDTO;
import com.smartinventory.notificationservice.dto.PageResponse;
import com.smartinventory.notificationservice.entity.Notification;
import com.smartinventory.notificationservice.entity.enums.NotificationStatus;
import com.smartinventory.notificationservice.entity.enums.NotificationType;
import com.smartinventory.notificationservice.exception.ResourceNotFoundException;
import com.smartinventory.notificationservice.entity.Bill;
import com.smartinventory.notificationservice.entity.Product;
import com.smartinventory.notificationservice.repository.BillRepository;
import com.smartinventory.notificationservice.repository.NotificationRepository;
import com.smartinventory.notificationservice.repository.ProductRepository;
import com.smartinventory.notificationservice.service.EmailService;
import com.smartinventory.notificationservice.service.NotificationService;
import com.smartinventory.notificationservice.service.mapper.NotificationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Production implementation of {@link NotificationService}.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final EmailService emailService;
    private final BillRepository billRepository;
    private final ProductRepository productRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   NotificationMapper notificationMapper,
                                   EmailService emailService,
                                   BillRepository billRepository,
                                   ProductRepository productRepository) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
        this.emailService = emailService;
        this.billRepository = billRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponseDTO> getNotifications(NotificationStatus status, NotificationType type, Pageable pageable) {
        log.debug("Fetching notifications with filter: status={}, type={}", status, type);
        Page<Notification> page;

        if (status != null && type != null) {
            page = notificationRepository.findByStatusAndType(status, type, pageable);
        } else if (status != null) {
            page = notificationRepository.findByStatus(status, pageable);
        } else if (type != null) {
            page = notificationRepository.findByType(type, pageable);
        } else {
            page = notificationRepository.findAll(pageable);
        }

        List<NotificationResponseDTO> content = page.getContent().stream()
                .map(this::toDTOWithResolvedEmail)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponseDTO getNotificationById(Long id) {
        log.debug("Fetching notification by ID: {}", id);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + id));
        return toDTOWithResolvedEmail(notification);
    }

    private NotificationResponseDTO toDTOWithResolvedEmail(Notification entity) {
        NotificationResponseDTO dto = notificationMapper.toDTO(entity);
        if (dto != null) {
            String resolvedEmail = resolveRecipientEmail(entity);
            if (resolvedEmail != null && !resolvedEmail.trim().isEmpty()) {
                dto.setRecipientEmail(resolvedEmail);
            }
        }
        return dto;
    }

    private String resolveRecipientEmail(Notification notification) {
        if (notification == null) {
            return null;
        }

        // 1. If the notification references a Bill, retrieve the email from the Bill's Customer
        if ("BILL".equalsIgnoreCase(notification.getReferenceType()) && notification.getReferenceId() != null && billRepository != null) {
            try {
                Optional<Bill> billOpt = billRepository.findById(notification.getReferenceId());
                if (billOpt.isPresent() && billOpt.get().getCustomer() != null) {
                    String customerEmail = billOpt.get().getCustomer().getEmail();
                    if (customerEmail != null && !customerEmail.trim().isEmpty()) {
                        return customerEmail.trim();
                    }
                }
            } catch (Exception ex) {
                log.warn("Could not retrieve customer email for Bill ID: {}", notification.getReferenceId(), ex);
            }
        }

        // 2. If the notification references a Supplier alert, retrieve the Supplier email
        if ("PRODUCT".equalsIgnoreCase(notification.getReferenceType()) && notification.getReferenceId() != null && productRepository != null) {
            try {
                Optional<com.smartinventory.notificationservice.entity.Product> prodOpt = productRepository.findById(notification.getReferenceId());
                if (prodOpt.isPresent() && prodOpt.get().getSupplier() != null) {
                    String supplierEmail = prodOpt.get().getSupplier().getEmail();
                    if (supplierEmail != null && !supplierEmail.trim().isEmpty()) {
                        return supplierEmail.trim();
                    }
                }
            } catch (Exception ex) {
                log.warn("Could not retrieve supplier email for Product ID: {}", notification.getReferenceId(), ex);
            }
        }

        return notification.getRecipientEmail();
    }

    @Override
    @Transactional
    public NotificationApprovalResponseDTO approveNotification(Long id) {
        log.info("Processing owner approval for Notification ID: {}", id);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + id));

        // Business Rule: Notification must have type = 'LOW_STOCK_ALERT' and status = 'PENDING'
        if (notification.getType() != NotificationType.LOW_STOCK_ALERT) {
            throw new IllegalStateException("Only LOW_STOCK_ALERT notifications can be approved. Current type: " + notification.getType());
        }
        if (notification.getStatus() != NotificationStatus.PENDING) {
            throw new IllegalStateException("Notification must be in PENDING status to approve. Current status: " + notification.getStatus());
        }

        // Sets status = APPROVED
        notification.setStatus(NotificationStatus.APPROVED);

        String targetEmail = resolveRecipientEmail(notification);
        if (targetEmail != null && !targetEmail.trim().isEmpty()) {
            notification.setRecipientEmail(targetEmail);
        }

        // Attempt dispatch via emailService
        try {
            emailService.sendEmail(notification.getRecipientEmail(), notification.getSubject(), notification.getBody());
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setErrorMessage(null);
            log.info("Notification ID: {} approved and successfully sent to {}", id, notification.getRecipientEmail());
        } catch (Exception ex) {
            log.error("Failed to dispatch email for notification ID: {}. Error: {}", id, ex.getMessage(), ex);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(ex.getMessage());
        }

        Notification saved = notificationRepository.save(notification);
        return notificationMapper.toApprovalDTO(saved);
    }

    @Override
    @Transactional
    public NotificationResponseDTO retryNotification(Long id) {
        log.info("Retrying email dispatch for failed Notification ID: {}", id);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + id));

        if (notification.getStatus() != NotificationStatus.FAILED) {
            throw new IllegalStateException("Only notifications with FAILED status can be retried. Current status: " + notification.getStatus());
        }

        String targetEmail = resolveRecipientEmail(notification);
        if (targetEmail != null && !targetEmail.trim().isEmpty()) {
            notification.setRecipientEmail(targetEmail);
        }

        try {
            emailService.sendEmail(notification.getRecipientEmail(), notification.getSubject(), notification.getBody());
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setErrorMessage(null);
            log.info("Retry succeeded for notification ID: {}", id);
        } catch (Exception ex) {
            log.error("Retry failed for notification ID: {}. Error: {}", id, ex.getMessage(), ex);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(ex.getMessage());
        }

        Notification saved = notificationRepository.save(notification);
        return toDTOWithResolvedEmail(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public long getActionableCount() {
        return notificationRepository.countActionableNotifications();
    }
}
