package com.smartinventory.notificationservice.event;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Consumer for RabbitMQ domain events.
 * Listens for:
 * 1. BillConfirmedEvent -> Persists PENDING INVOICE_EMAIL draft waiting for payment.
 * 2. PaymentCompletedEvent -> Updates existing INVOICE_EMAIL notification with payment details and immediately sends invoice email to customer.
 * 3. LowStockDetectedEvent -> Persists PENDING LOW_STOCK_ALERT notification awaiting manual owner approval.
 */
@Component
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final BillRepository billRepository;
    private final com.smartinventory.notificationservice.repository.ProductRepository productRepository;

    public EventConsumer(NotificationRepository notificationRepository,
                         EmailService emailService,
                         BillRepository billRepository,
                         com.smartinventory.notificationservice.repository.ProductRepository productRepository) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
        this.billRepository = billRepository;
        this.productRepository = productRepository;
    }

    /**
     * Consumes BillConfirmed event from bill.confirmed.queue.
     * Persists a draft INVOICE_EMAIL notification record in PENDING status waiting for payment.
     * Does NOT send emails.
     *
     * @param event BillConfirmedEvent
     */
    @RabbitListener(queues = "${app.rabbitmq.queue.bill-confirmed:bill.confirmed.queue}")
    @Transactional
    public void handleBillConfirmed(BillConfirmedEvent event) {
        if (event == null || event.getBillId() == null) {
            log.warn("Received null or invalid BillConfirmedEvent. Ignoring.");
            return;
        }

        log.info("Received BillConfirmedEvent for Bill ID: {}, Number: {}, Customer: {}, Email: {}",
                event.getBillId(), event.getBillNumber(), event.getCustomerName(), event.getCustomerEmail());

        String recipientEmail = event.getCustomerEmail();
        if ((recipientEmail == null || recipientEmail.trim().isEmpty()) && event.getBillId() != null && billRepository != null) {
            recipientEmail = billRepository.findById(event.getBillId())
                    .filter(b -> b.getCustomer() != null)
                    .map(b -> b.getCustomer().getEmail())
                    .orElse(null);
        }

        // If customer has no email (e.g. Walk-in Customer):
        // Requirements 4 & 5: Skip queue completely, Status: SKIPPED (No Email), NOT Pending.
        // DO NOT show Pending Email, DO NOT increase notification badge.
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            log.info("Bill ID: {} confirmed for customer with no email. Recording status as SKIPPED (No Email).", event.getBillId());
            Notification notification = new Notification();
            notification.setType(NotificationType.INVOICE_EMAIL);
            notification.setStatus(NotificationStatus.SKIPPED);
            notification.setRecipientEmail(null);
            notification.setSubject("Invoice - " + event.getBillNumber());
            notification.setBody("No Email Available");
            notification.setReferenceId(event.getBillId());
            notification.setReferenceType("BILL");
            notification.setErrorMessage("Skipped: Customer has no email address");

            notificationRepository.save(notification);
            return;
        }

        Notification notification = new Notification();
        notification.setType(NotificationType.INVOICE_EMAIL);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setRecipientEmail(recipientEmail);
        notification.setSubject("Invoice - " + event.getBillNumber());
        notification.setBody(String.format(
                "Dear %s,\n\nYour invoice %s has been generated for total amount ₹%s on %s.\nStatus: Awaiting Payment\n\nThank you for your business!",
                event.getCustomerName() != null ? event.getCustomerName() : "Customer",
                event.getBillNumber(),
                event.getTotalAmount(),
                event.getBillDate()
        ));
        notification.setReferenceId(event.getBillId());
        notification.setReferenceType("BILL");

        notificationRepository.save(notification);
        log.info("Persisted draft INVOICE_EMAIL notification record ID: {} for confirmed Bill: {} with recipient: {}",
                notification.getId(), event.getBillNumber(), notification.getRecipientEmail());
    }

    /**
     * Consumes PaymentCompleted event from payment.completed.queue.
     * Finds the existing INVOICE_EMAIL notification for the bill, updates it with payment details,
     * and immediately dispatches the invoice email to the customer without manual approval.
     *
     * @param event PaymentCompletedEvent
     */
    @RabbitListener(queues = "${app.rabbitmq.queue.payment-completed:payment.completed.queue}")
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        if (event == null || event.getPaymentId() == null) {
            log.warn("Received null or invalid PaymentCompletedEvent. Ignoring.");
            return;
        }

        log.info("Received PaymentCompletedEvent for paymentId: {}, Bill ID: {}, Bill Number: {}, Customer: {}, Email: {}",
                event.getPaymentId(), event.getBillId(), event.getBillNumber(), event.getCustomerName(), event.getCustomerEmail());

        String recipientEmail = event.getCustomerEmail();
        if ((recipientEmail == null || recipientEmail.trim().isEmpty()) && event.getBillId() != null && billRepository != null) {
            recipientEmail = billRepository.findById(event.getBillId())
                    .filter(b -> b.getCustomer() != null)
                    .map(b -> b.getCustomer().getEmail())
                    .orElse(null);
        }

        // If customer has no email, mark notification as SKIPPED and do not send email
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            log.info("Payment completed for Bill ID: {}, but customer has no email address. Marking as SKIPPED.", event.getBillId());
            Notification existing = null;
            if (event.getBillId() != null) {
                existing = notificationRepository
                        .findFirstByReferenceTypeAndReferenceIdAndTypeOrderByCreatedAtDesc(
                                "BILL", event.getBillId(), NotificationType.INVOICE_EMAIL)
                        .orElse(null);
            }
            if (existing != null) {
                existing.setStatus(NotificationStatus.SKIPPED);
                existing.setRecipientEmail(null);
                existing.setErrorMessage("Skipped: Customer has no email address");
                notificationRepository.save(existing);
            }
            return;
        }

        // 1. Find existing INVOICE_EMAIL notification for the same bill
        Notification notification = null;
        if (event.getBillId() != null) {
            notification = notificationRepository
                    .findFirstByReferenceTypeAndReferenceIdAndTypeOrderByCreatedAtDesc(
                            "BILL", event.getBillId(), NotificationType.INVOICE_EMAIL)
                    .orElse(null);
        }

        // Fallback: If no existing draft invoice notification exists, create one
        if (notification == null) {
            log.info("No prior INVOICE_EMAIL found for Bill ID: {}. Creating a new INVOICE_EMAIL notification.", event.getBillId());
            notification = new Notification();
            notification.setType(NotificationType.INVOICE_EMAIL);
            notification.setReferenceId(event.getBillId());
            notification.setReferenceType("BILL");
            notification.setRecipientEmail(recipientEmail);
        } else {
            notification.setRecipientEmail(recipientEmail);
        }

        // 2. Set Subject: "Invoice - <Bill Number>"
        String billNumber = event.getBillNumber() != null ? event.getBillNumber() : "N/A";
        notification.setSubject("Invoice - " + billNumber);

        // Format dates and financial fields
        String customerName = event.getCustomerName() != null ? event.getCustomerName() : "Valued Customer";
        String paymentDateStr = event.getPaymentDate() != null
                ? event.getPaymentDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String billDateStr = event.getPaymentDate() != null
                ? event.getPaymentDate().toLocalDate().toString()
                : LocalDate.now().toString();
        String totalAmountStr = event.getAmountPaid() != null ? event.getAmountPaid().toPlainString() : "0.00";
        String paymentMethod = event.getPaymentMethod() != null ? event.getPaymentMethod() : "UPI / Electronic";
        String transactionId = event.getPaymentId() != null ? String.valueOf(event.getPaymentId()) : "N/A";

        // Build professional HTML invoice body
        String htmlBody = buildInvoiceEmailHtml(
                customerName,
                billNumber,
                billDateStr,
                totalAmountStr, // Subtotal
                "0.00",         // Discount
                "0.00",         // Tax
                totalAmountStr, // Total Amount
                paymentMethod,
                transactionId,
                paymentDateStr
        );
        notification.setBody(htmlBody);

        // 3. Immediately dispatch invoice email to customer (no manual approval required)
        try {
            emailService.sendEmail(notification.getRecipientEmail(), notification.getSubject(), notification.getBody());
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setErrorMessage(null);
            log.info("Invoice email successfully sent to customer: {} for Bill: {}", notification.getRecipientEmail(), billNumber);

            // Update bill.invoice_sent = true if bill record exists
            if (event.getBillId() != null && billRepository != null) {
                billRepository.findById(event.getBillId()).ifPresent(bill -> {
                    bill.setInvoiceSent(true);
                    billRepository.save(bill);
                    log.info("Updated bill ID: {} invoice_sent = true", bill.getId());
                });
            }
        } catch (Exception ex) {
            log.error("Failed to send invoice email for Bill {}: {}", billNumber, ex.getMessage(), ex);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(ex.getMessage());
        }

        // 4. Save updated notification record
        notificationRepository.save(notification);
        log.info("Persisted updated INVOICE_EMAIL Notification ID: {} with status: {}", notification.getId(), notification.getStatus());
    }

    /**
     * Consumes LowStockDetected event from stock.low.queue.
     * Persists LOW_STOCK_ALERT notification in PENDING status.
     * Does NOT automatically send emails. Awaits manual PUT /api/v1/notifications/{id}/approve.
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
        if ((supplierEmail == null || supplierEmail.trim().isEmpty()) && payload.getProductId() != null && productRepository != null) {
            supplierEmail = productRepository.findById(payload.getProductId())
                    .filter(p -> p.getSupplier() != null)
                    .map(p -> p.getSupplier().getEmail())
                    .orElse(null);
        }
        if (supplierEmail == null) {
            supplierEmail = "";
        }

        String subject = "Purchase Request - Low Stock";
        int currentQuantity = payload.getCurrentQuantity() != null ? payload.getCurrentQuantity() : 0;
        int reorderLevel = payload.getReorderLevel() != null ? payload.getReorderLevel() : 0;
        int requestedQuantity = Math.max(reorderLevel * 2 - currentQuantity, reorderLevel > 0 ? reorderLevel : 50);
        String requestDate = (event.getTimestamp() != null ? event.getTimestamp().toLocalDate() : LocalDate.now()).toString();

        String body = buildLowStockEmailHtml(
                payload.getSupplierName() != null ? payload.getSupplierName() : "Supplier",
                payload.getProductName() != null ? payload.getProductName() : "Product",
                payload.getSku() != null ? payload.getSku() : "N/A",
                currentQuantity,
                reorderLevel,
                requestedQuantity,
                requestDate
        );

        Notification notification = new Notification();
        notification.setType(NotificationType.LOW_STOCK_ALERT);
        notification.setStatus(NotificationStatus.PENDING); // Awaits owner approval per business workflow
        notification.setRecipientEmail(supplierEmail);
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setReferenceId(payload.getProductId());
        notification.setReferenceType("PRODUCT");

        notificationRepository.save(notification);
        log.info("Persisted PENDING low-stock notification for product ID: {}", payload.getProductId());
    }

    private String buildInvoiceEmailHtml(
            String customerName,
            String billNumber,
            String billDate,
            String subtotal,
            String discount,
            String tax,
            String totalAmount,
            String paymentMethod,
            String transactionId,
            String paymentDate) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <style>\n" +
                "    body { font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f4f7f6; margin: 0; padding: 20px; color: #333; }\n" +
                "    .container { max-width: 650px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }\n" +
                "    .header { background: linear-gradient(135deg, #1e3a8a, #3b82f6); color: #ffffff; padding: 24px 32px; }\n" +
                "    .header h1 { margin: 0 0 6px 0; font-size: 24px; font-weight: 600; }\n" +
                "    .header p { margin: 0; font-size: 14px; opacity: 0.9; }\n" +
                "    .content { padding: 32px; }\n" +
                "    .greeting { font-size: 16px; margin-bottom: 20px; line-height: 1.5; }\n" +
                "    .details-table { width: 100%; border-collapse: collapse; margin: 20px 0; }\n" +
                "    .details-table th, .details-table td { padding: 12px 14px; text-align: left; border-bottom: 1px solid #e5e7eb; font-size: 14px; }\n" +
                "    .details-table th { background-color: #f9fafb; color: #4b5563; font-weight: 600; width: 40%; }\n" +
                "    .items-table { width: 100%; border-collapse: collapse; margin: 20px 0; }\n" +
                "    .items-table th { background-color: #f3f4f6; color: #374151; padding: 10px 14px; text-align: left; font-size: 13px; text-transform: uppercase; letter-spacing: 0.05em; }\n" +
                "    .items-table td { padding: 12px 14px; border-bottom: 1px solid #e5e7eb; font-size: 14px; }\n" +
                "    .total-row { font-weight: 700; font-size: 16px; color: #1e3a8a; }\n" +
                "    .badge { display: inline-block; background-color: #10b981; color: #ffffff; padding: 4px 10px; border-radius: 9999px; font-size: 12px; font-weight: 600; }\n" +
                "    .footer { background-color: #f9fafb; padding: 20px 32px; text-align: center; font-size: 12px; color: #6b7280; border-top: 1px solid #e5e7eb; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"container\">\n" +
                "    <div class=\"header\">\n" +
                "      <h1>Tax Invoice & Payment Receipt</h1>\n" +
                "      <p>Thank you for choosing Smart Inventory & Billing Management</p>\n" +
                "    </div>\n" +
                "    <div class=\"content\">\n" +
                "      <p class=\"greeting\">Dear <strong>" + customerName + "</strong>,</p>\n" +
                "      <p>Your payment has been successfully processed and your invoice is confirmed. Please find your complete bill and transaction details below:</p>\n" +
                "\n" +
                "      <table class=\"details-table\">\n" +
                "        <tr><th>Bill Number</th><td><strong>" + billNumber + "</strong></td></tr>\n" +
                "        <tr><th>Bill Date</th><td>" + billDate + "</td></tr>\n" +
                "        <tr><th>Customer Name</th><td>" + customerName + "</td></tr>\n" +
                "        <tr><th>Payment Status</th><td><span class=\"badge\">PAID / COMPLETED</span></td></tr>\n" +
                "        <tr><th>Payment Method</th><td>" + paymentMethod + "</td></tr>\n" +
                "        <tr><th>Transaction ID</th><td>#" + transactionId + "</td></tr>\n" +
                "        <tr><th>Payment Date</th><td>" + paymentDate + "</td></tr>\n" +
                "      </table>\n" +
                "\n" +
                "      <h3 style=\"margin-top: 24px; color: #1f2937; font-size: 16px;\">Items Summary</h3>\n" +
                "      <table class=\"items-table\">\n" +
                "        <thead>\n" +
                "          <tr><th>Description</th><th style=\"text-align: right;\">Amount</th></tr>\n" +
                "        </thead>\n" +
                "        <tbody>\n" +
                "          <tr>\n" +
                "            <td>Purchased Items & Inventory (Bill " + billNumber + ")</td>\n" +
                "            <td style=\"text-align: right;\">₹" + subtotal + "</td>\n" +
                "          </tr>\n" +
                "          <tr>\n" +
                "            <td style=\"color: #6b7280;\">Subtotal</td>\n" +
                "            <td style=\"text-align: right;\">₹" + subtotal + "</td>\n" +
                "          </tr>\n" +
                "          <tr>\n" +
                "            <td style=\"color: #6b7280;\">Discount</td>\n" +
                "            <td style=\"text-align: right;\">₹" + discount + "</td>\n" +
                "          </tr>\n" +
                "          <tr>\n" +
                "            <td style=\"color: #6b7280;\">Tax</td>\n" +
                "            <td style=\"text-align: right;\">₹" + tax + "</td>\n" +
                "          </tr>\n" +
                "          <tr class=\"total-row\">\n" +
                "            <td><strong>Total Amount</strong></td>\n" +
                "            <td style=\"text-align: right;\"><strong>₹" + totalAmount + "</strong></td>\n" +
                "          </tr>\n" +
                "        </tbody>\n" +
                "      </table>\n" +
                "\n" +
                "      <p style=\"margin-top: 24px; font-size: 14px; color: #4b5563;\">If you have any questions concerning this invoice, please reach out to our customer care team.</p>\n" +
                "    </div>\n" +
                "    <div class=\"footer\">\n" +
                "      <p>&copy; 2026 Smart Inventory & Billing Management System. All rights reserved.</p>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>";
    }

    private String buildLowStockEmailHtml(
            String supplierName,
            String productName,
            String sku,
            Integer currentStock,
            Integer reorderLevel,
            Integer requestedQuantity,
            String requestDate) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <style>\n" +
                "    body { font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f4f7f6; margin: 0; padding: 20px; color: #333; }\n" +
                "    .container { max-width: 650px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }\n" +
                "    .header { background: linear-gradient(135deg, #b91c1c, #ef4444); color: #ffffff; padding: 24px 32px; }\n" +
                "    .header h1 { margin: 0 0 6px 0; font-size: 22px; font-weight: 600; }\n" +
                "    .header p { margin: 0; font-size: 14px; opacity: 0.9; }\n" +
                "    .content { padding: 32px; }\n" +
                "    .greeting { font-size: 16px; margin-bottom: 20px; line-height: 1.5; }\n" +
                "    .details-table { width: 100%; border-collapse: collapse; margin: 20px 0; }\n" +
                "    .details-table th, .details-table td { padding: 12px 14px; text-align: left; border-bottom: 1px solid #e5e7eb; font-size: 14px; }\n" +
                "    .details-table th { background-color: #f9fafb; color: #4b5563; font-weight: 600; width: 40%; }\n" +
                "    .alert-badge { display: inline-block; background-color: #fee2e2; color: #991b1b; padding: 4px 10px; border-radius: 6px; font-weight: 600; font-size: 13px; }\n" +
                "    .highlight-row { background-color: #fef2f2; font-weight: 600; }\n" +
                "    .footer { background-color: #f9fafb; padding: 20px 32px; text-align: center; font-size: 12px; color: #6b7280; border-top: 1px solid #e5e7eb; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"container\">\n" +
                "    <div class=\"header\">\n" +
                "      <h1>Purchase Request - Low Stock</h1>\n" +
                "      <p>Automated Inventory Alert & Purchase Requisition</p>\n" +
                "    </div>\n" +
                "    <div class=\"content\">\n" +
                "      <p class=\"greeting\">Dear <strong>" + supplierName + "</strong>,</p>\n" +
                "      <p>Our warehouse inventory levels have dropped to or below the designated reorder threshold. Please accept this formal purchase request to replenish stock for the product specified below:</p>\n" +
                "\n" +
                "      <table class=\"details-table\">\n" +
                "        <tr><th>Supplier Name</th><td>" + supplierName + "</td></tr>\n" +
                "        <tr><th>Product Name</th><td><strong>" + productName + "</strong></td></tr>\n" +
                "        <tr><th>SKU</th><td><code>" + sku + "</code></td></tr>\n" +
                "        <tr class=\"highlight-row\"><th>Current Stock</th><td><span class=\"alert-badge\">" + currentStock + " units</span></td></tr>\n" +
                "        <tr><th>Reorder Level</th><td>" + reorderLevel + " units</td></tr>\n" +
                "        <tr><th>Requested Quantity</th><td><strong>" + requestedQuantity + " units</strong></td></tr>\n" +
                "        <tr><th>Request Date</th><td>" + requestDate + "</td></tr>\n" +
                "      </table>\n" +
                "\n" +
                "      <p style=\"margin-top: 24px; font-size: 14px; color: #4b5563;\">Kindly confirm receipt of this purchase request and advise on the estimated delivery schedule at your earliest convenience.</p>\n" +
                "      <p style=\"font-size: 14px; color: #4b5563;\">Best regards,<br><strong>Smart Inventory Procurement Team</strong></p>\n" +
                "    </div>\n" +
                "    <div class=\"footer\">\n" +
                "      <p>&copy; 2026 Smart Inventory & Billing Management System. All rights reserved.</p>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>";
    }

    /**
     * Consumes BillCancelled event from bill.cancelled.queue.
     * Rules (Requirement 6):
     * If invoice becomes Cancelled BEFORE mail sent:
     * Cancel queued email, remove pending notification, mark audit Cancelled (reason: Invoice Cancelled).
     * If email already sent: keep audit as Sent, do NOT delete history.
     *
     * @param event BillCancelledEvent
     */
    @RabbitListener(queues = "${app.rabbitmq.queue.bill-cancelled:bill.cancelled.queue}")
    @Transactional
    public void handleBillCancelled(BillCancelledEvent event) {
        if (event == null || event.getBillId() == null) {
            log.warn("Received null or invalid BillCancelledEvent. Ignoring.");
            return;
        }

        log.info("Handling BillCancelledEvent for Bill ID: {}, Number: {}", event.getBillId(), event.getBillNumber());

        java.util.List<Notification> pendingNotifications = notificationRepository
                .findByReferenceTypeAndReferenceIdAndStatus("BILL", event.getBillId(), NotificationStatus.PENDING);

        for (Notification notification : pendingNotifications) {
            notification.setStatus(NotificationStatus.CANCELLED);
            notification.setErrorMessage("Invoice Cancelled");
            notificationRepository.save(notification);
            log.info("Cancelled pending notification ID: {} for cancelled Bill ID: {}", notification.getId(), event.getBillId());
        }
    }
}
