package com.smartinventory.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Category entity representing product taxonomy classifications.
 * Mapped to 'category' table in PostgreSQL.
 * Strictly adheres to Section 4.1 of DATABASE_SPECIFICATION.md.
 */
@Entity
@Table(
        name = "category",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_category_name", columnNames = "name")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
