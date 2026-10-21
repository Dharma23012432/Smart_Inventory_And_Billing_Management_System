package com.smartinventory.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * PurchaseItem entity representing line items in a purchase order.
 * Strictly adheres to Section 4.5 of DATABASE_SPECIFICATION.md and schema.sql.
 */
@Entity
@Table(
        name = "purchase_item",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_purchase_item_purchase_product", columnNames = {"purchase_id", "product_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"purchase", "product"})
public class PurchaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "purchase_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_purchase_item_purchase")
    )
    private Purchase purchase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_purchase_item_product")
    )
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "total_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCost;
}
