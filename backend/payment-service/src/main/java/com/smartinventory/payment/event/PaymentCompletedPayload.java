package com.smartinventory.payment.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payload carried by PaymentCompletedEvent.
 * Matches API_SPECIFICATION.md Section 7.1.
 */
public class PaymentCompletedPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long paymentId;
    private Long billId;
    private String billNumber;
    private BigDecimal amountPaid;
    private String paymentMethod;
    private String referenceNumber;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private LocalDateTime paidAt;

    public PaymentCompletedPayload() {
    }

    public PaymentCompletedPayload(Long paymentId, Long billId, String billNumber, BigDecimal amountPaid,
                                   String paymentMethod, String referenceNumber, Long customerId,
                                   String customerName, String customerEmail, String customerPhone,
                                   LocalDateTime paidAt) {
        this.paymentId = paymentId;
        this.billId = billId;
        this.billNumber = billNumber;
        this.amountPaid = amountPaid;
        this.paymentMethod = paymentMethod;
        this.referenceNumber = referenceNumber;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.paidAt = paidAt;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
