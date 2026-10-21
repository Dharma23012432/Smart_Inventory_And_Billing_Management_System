package com.smartinventory.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Setting entity representing global runtime application configuration.
 * Strictly adheres to Section 4.4 of DATABASE_SPECIFICATION.md and schema.sql.
 */
@Entity
@Table(
        name = "settings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_settings_setting_key", columnNames = "setting_key")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class Setting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String key;

    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
