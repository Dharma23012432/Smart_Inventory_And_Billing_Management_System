package com.smartinventory.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Inventory entity tracking real-time stock levels and reorder parameters.
 * Maintains a 1:1 relationship with Product.
 * Strictly adheres to Section 4.3 of DATABASE_SPECIFICATION.md.
 */
@Entity
@Table(
        name = "inventory",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_inventory_product_id", columnNames = "product_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "product")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_inventory_product")
    )
    private Product product;

    @Column(name = "quantity_on_hand", nullable = false)
    @Builder.Default
    private Integer quantityOnHand = 0;

    @Column(name = "reorder_level", nullable = false)
    @Builder.Default
    private Integer reorderLevel = 10;

    @Column(name = "max_stock_level", nullable = false)
    @Builder.Default
    private Integer maxStockLevel = 100;

    /**
     * Optimistic locking version column to prevent race conditions and stock overselling.
     */
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.lastUpdatedAt == null) {
            this.lastUpdatedAt = Instant.now();
        }
        if (this.quantityOnHand == null) {
            this.quantityOnHand = 0;
        }
        if (this.reorderLevel == null) {
            this.reorderLevel = 10;
        }
        if (this.maxStockLevel == null) {
            this.maxStockLevel = 100;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdatedAt = Instant.now();
    }

    /**
     * Helper to determine if product is currently at or below reorder threshold.
     */
    public boolean isLowStock() {
        return this.quantityOnHand != null && this.reorderLevel != null && this.quantityOnHand <= this.reorderLevel;
    }
}
