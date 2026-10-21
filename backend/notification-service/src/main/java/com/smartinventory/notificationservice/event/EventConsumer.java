package com.smartinventory.notificationservice.event;

import com.smartinventory.notificationservice.entity.Bill;
import com.smartinventory.notificationservice.entity.Notification;
import com.smartinventory.notificationservice.entity.enums.NotificationStatus;
import com.smartinventory.notificationservice.entity.enums.NotificationType;
import com.smartinventory.notificationservice.repository.BillRepository;
import com.smartinventory.notificationservice.repository.NotificationRepository;
import com.smartinventory.notificationservice.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Consumer for RabbitMQ domain events.
 * Listens for:
 * 1. PaymentCompletedEvent -> Generates invoice email, sends immediately, sets bill.invoice_sent = true.
 * 2. LowStockDetectedEvent -> Persists PENDING notification awaiting owner approval.
 */
@Component
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    private final NotificationRepository notificationRepository;
    private final BillRepository billRepository;
    private final EmailService emailService;

    public EventConsumer(NotificationRepository notificationRepository,
                         BillRepository billRepository,
                         EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.billRepository = billRepository;
        this.emailService = emailService;
    }

    /**
     * Consumes PaymentCompleted event from payment.completed.queue.
     *
     * @param event PaymentCompletedEvent
     */
    @RabbitListener(queues = "${app.rabbitmq.queue.payment-completed:payment.completed.queue}")
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        if (event == null || event.getPayload() == null) {
            log.warn("Received null PaymentCompletedEvent or payload. Ignoring.");
            return;
        }

        PaymentCompletedPayload payload = event.getPayload();
        log.info("Handling PaymentCompleted event for Bill: {}, Customer: {}",
                payload.getBillNumber(), payload.getCustomerEmail());

        String customerEmail = payload.getCustomerEmail();
        if (customerEmail == null || customerEmail.trim().isEmpty()) {
            log.warn("No customer email provided for Bill ID: {}. Skipping invoice email dispatch.", payload.getBillId());
            return;
        }

        String subject = "Payment Receipt & Invoice: " + payload.getBillNumber();
        String body = String.format(
                "Dear %s,\n\nThank you for your payment of %s towards invoice %s using %s.\nTransaction Reference: %s\n\nBest regards,\nSmart Inventory Team",
                payload.getCustomerName() != null ? payload.getCustomerName() : "Valued Customer",
                payload.getAmountPaid(),
                payload.getBillNumber(),
                payload.getPaymentMethod(),
                payload.getReferenceNumber() != null ? payload.getReferenceNumber() : "N/A"
        );

        Notification notification = new Notification();
        notification.setType(NotificationType.INVOICE_EMAIL);
        notification.setRecipientEmail(customerEmail);
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setReferenceId(payload.getBillId());
        notification.setReferenceType("BILL");

        try {
            emailService.sendEmail(customerEmail, subject, body);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setErrorMessage(null);
            log.info("Successfully sent invoice email for Bill ID: {}", payload.getBillId());

            // Mark bill.invoice_sent = true
            if (payload.getBillId() != null) {
                billRepository.findById(payload.getBillId()).ifPresent(bill -> {
                    bill.setInvoiceSent(true);
                    billRepository.save(bill);
                });
            }
        } catch (Exception ex) {
            log.error("Failed to send invoice email for Bill ID: {}. Error: {}", payload.getBillId(), ex.getMessage(), ex);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(ex.getMessage());
        }

        notificationRepository.save(notification);
    }

    /**
     * Consumes LowStockDetected event from stock.low.queue.
     *
     * @param event LowStockDetectedEvent
     */
    @RabbitListener(queues = "${app.rabbitmq.queue.low-stock:stock.low.queue}")
    @Transactional
    public void handleLowStockDetected(LowStockDetectedEvent event) {
        if (event == null || event.getPayload() == null) {
            log.warn("Received null LowStockDetectedEvent or payload. Ignoring.");
            return;
        }

        LowStockDetectedPayload payload = event.getPayload();
        log.info("Handling LowStockDetected event for Product: '{}' (ID: {}), Current Quantity: {}, Reorder Level: {}",
                payload.getProductName(), payload.getProductId(), payload.getCurrentQuantity(), payload.getReorderLevel());

        String supplierEmail = payload.getSupplierEmail();
        if (supplierEmail == null || supplierEmail.trim().isEmpty()) {
            supplierEmail = "procurement@smartinventory.com"; // Default contact if supplier email not configured
        }

        String subject = String.format("Low Stock Alert: Purchase Request for %s (%s)", payload.getProductName(), payload.getSku());
        String body = String.format(
                "Dear %s,\n\nThe stock level for product '%s' (SKU: %s) has reached %d units, which is at or below the reorder threshold of %d units.\n\nPlease accept this notification as an official purchase order request.\n\nRegards,\nSmart Inventory Procurement",
                payload.getSupplierName() != null ? payload.getSupplierName() : "Supplier",
                payload.getProductName(),
                payload.getSku(),
                payload.getCurrentQuantity(),
                payload.getReorderLevel()
        );

        Notification notification = new Notification();
        notification.setType(NotificationType.LOW_STOCK_ALERT);
        notification.setStatus(NotificationStatus.PENDING); // Awaits owner approval per Section 7.2
        notification.setRecipientEmail(supplierEmail);
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setReferenceId(payload.getProductId());
        notification.setReferenceType("PRODUCT");

        notificationRepository.save(notification);
        log.info("Persisted PENDING low-stock notification for product ID: {}", payload.getProductId());
    }
}
