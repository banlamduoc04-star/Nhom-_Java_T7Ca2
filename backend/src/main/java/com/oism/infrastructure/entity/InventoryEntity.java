package com.oism.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "inventory_items",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenant_sku", columnNames = {"tenantId", "skuCode"})
    },
    indexes = {
        @Index(name = "idx_tenant_sku", columnList = "tenantId, skuCode")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 64)
    private String skuCode;

    @Column(nullable = false)
    private Long quantity;

    @Column(precision = 18, scale = 4, nullable = false)
    private BigDecimal averageUnitCost;

    @Version
    private Long version; // Optimistic Locking chống race condition tầng DB (Bug-INV-01)

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}