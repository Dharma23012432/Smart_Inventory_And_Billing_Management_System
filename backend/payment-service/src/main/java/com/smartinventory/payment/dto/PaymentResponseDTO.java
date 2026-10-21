package com.smartinventory.payment.dto;

import com.smartinventory.payment.entity.enums.PaymentMethod;
import com.smartinventory.payment.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing the payment details returned to API consumers.
 * Matches API_SPECIFICATION.md Section 3.8.
 */
public class PaymentResponseDTO {

    private Long paymentId;
    private Long billId;
    private String billNumber;
    private BigDecimal amountPaid;
    private PaymentMethod paymentMethod;
    private String referenceNumber;
    private PaymentStatus status;
    private LocalDateTime paymentDate;

    /**
     * Default constructor.
     */
    public PaymentResponseDTO() {
    }

    /**
     * Parameterized constructor.
     *
     * @param paymentId       the unique payment identifier
     * @param billId          the ID of the associated bill
     * @param billNumber      the human-readable bill invoice number
     * @param amountPaid      the total amount paid
     * @param paymentMethod   the method used for payment
     * @param referenceNumber transaction reference or receipt number
     * @param status          payment status (COMPLETED, FAILED, REFUNDED)
     * @param paymentDate     the timestamp when payment was recorded
     */
    public PaymentResponseDTO(Long paymentId, Long billId, String billNumber, BigDecimal amountPaid,
                              PaymentMethod paymentMethod, String referenceNumber,
                              PaymentStatus status, LocalDateTime paymentDate) {
        this.paymentId = paymentId;
        this.billId = billId;
        this.billNumber = billNumber;
        this.amountPaid = amountPaid;
        this.paymentMethod = paymentMethod;
        this.referenceNumber = referenceNumber;
        this.status = status;
        this.paymentDate = paymentDate;
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

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }
}
