package com.smartinventory.inventoryservice.entity;

import com.smartinventory.inventoryservice.entity.enums.BillStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Bill entity representing customer sales invoices and transactions.
 * Strictly adheres to Section 4.6 of DATABASE_SPECIFICATION.md and schema.sql.
 */
@Entity
@Table(
        name = "bill",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_bill_bill_number", columnNames = "bill_number")
        },
        indexes = {
                @Index(name = "idx_bill_customer", columnList = "customer_id"),
                @Index(name = "idx_bill_status", columnList = "status"),
                @Index(name = "idx_bill_date", columnList = "bill_date"),
                @Index(name = "idx_bill_number", columnList = "bill_number")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"customer", "items"})
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_number", nullable = false, unique = true, length = 50)
    private String billNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "customer_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_bill_customer")
    )
    private Customer customer;

    @Column(name = "bill_date", nullable = false)
    private LocalDate billDate;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BillStatus status = BillStatus.DRAFT;

    @Column(name = "invoice_sent", nullable = false)
    @Builder.Default
    private Boolean invoiceSent = false;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(
            mappedBy = "bill",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<BillItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = BillStatus.DRAFT;
        }
        if (this.billDate == null) {
            this.billDate = LocalDate.now();
        }
        if (this.invoiceSent == null) {
            this.invoiceSent = false;
        }
        if (this.subtotal == null) {
            this.subtotal = BigDecimal.ZERO;
        }
        if (this.discountAmount == null) {
            this.discountAmount = BigDecimal.ZERO;
        }
        if (this.taxAmount == null) {
            this.taxAmount = BigDecimal.ZERO;
        }
        if (this.totalAmount == null) {
            this.totalAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Helper method to add a line item with bidirectional mapping.
     *
     * @param item BillItem to attach
     */
    public void addItem(BillItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
        item.setBill(this);
    }

    /**
     * Helper method to remove a line item with bidirectional mapping.
     *
     * @param item BillItem to remove
     */
    public void removeItem(BillItem item) {
        if (this.items != null) {
            this.items.remove(item);
            item.setBill(null);
        }
    }
}
