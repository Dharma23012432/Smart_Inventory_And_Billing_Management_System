package com.smartinventory.notificationservice.entity;

import jakarta.persistence.*;

/**
 * Bill entity representation in Notification Service.
 * Used for setting invoice_sent = true upon successful invoice email dispatch.
 */
@Entity
@Table(name = "bill")
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_number", nullable = false, unique = true, length = 50)
    private String billNumber;

    @Column(name = "invoice_sent", nullable = false)
    private Boolean invoiceSent = false;

    public Bill() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public Boolean getInvoiceSent() {
        return invoiceSent;
    }

    public void setInvoiceSent(Boolean invoiceSent) {
        this.invoiceSent = invoiceSent;
    }
}
