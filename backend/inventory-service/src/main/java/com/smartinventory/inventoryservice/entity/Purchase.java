package com.smartinventory.inventoryservice.entity;

import com.smartinventory.inventoryservice.entity.enums.PurchaseStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Purchase entity representing purchase orders placed with suppliers.
 * Strictly adheres to Section 4.5 of DATABASE_SPECIFICATION.md and schema.sql.
 */
@Entity
@Table(
        name = "purchase",
        indexes = {
                @Index(name = "idx_purchase_supplier", columnList = "supplier_id"),
                @Index(name = "idx_purchase_status", columnList = "status"),
                @Index(name = "idx_purchase_date", columnList = "purchase_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"supplier", "items"})
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "supplier_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_purchase_supplier")
    )
    private Supplier supplier;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PurchaseStatus status = PurchaseStatus.PENDING;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(
            mappedBy = "purchase",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<PurchaseItem> items = new ArrayList<>();

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
            this.status = PurchaseStatus.PENDING;
        }
        if (this.purchaseDate == null) {
            this.purchaseDate = LocalDate.now();
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
     * Helper method to add a purchase line item ensuring bidirectional mapping.
     *
     * @param item PurchaseItem to add
     */
    public void addItem(PurchaseItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
        item.setPurchase(this);
    }

    /**
     * Helper method to remove a purchase line item ensuring bidirectional mapping.
     *
     * @param item PurchaseItem to remove
     */
    public void removeItem(PurchaseItem item) {
        if (this.items != null) {
            this.items.remove(item);
            item.setPurchase(null);
        }
    }
}
