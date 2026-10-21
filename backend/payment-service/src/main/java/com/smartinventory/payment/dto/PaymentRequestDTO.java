package com.smartinventory.payment.dto;

import com.smartinventory.payment.entity.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Data Transfer Object for processing a payment against a confirmed bill.
 * Contains validated fields according to API_SPECIFICATION.md Section 3.8.
 */
public class PaymentRequestDTO {

    @NotNull(message = "Bill ID is required")
    private Long billId;

    @NotNull(message = "Amount paid is required")
    @DecimalMin(value = "0.01", message = "Amount paid must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Amount paid must have at most 10 integer digits and 2 decimal places")
    private BigDecimal amountPaid;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @Size(max = 100, message = "Reference number cannot exceed 100 characters")
    private String referenceNumber;

    /**
     * Default constructor for Jackson serialization.
     */
    public PaymentRequestDTO() {
    }

    /**
     * Parameterized constructor.
     *
     * @param billId          the ID of the bill
     * @param amountPaid      the amount paid
     * @param paymentMethod   the method of payment
     * @param referenceNumber the transaction or reference number
     */
    public PaymentRequestDTO(Long billId, BigDecimal amountPaid, PaymentMethod paymentMethod, String referenceNumber) {
        this.billId = billId;
        this.amountPaid = amountPaid;
        this.paymentMethod = paymentMethod;
        this.referenceNumber = referenceNumber;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
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
}
