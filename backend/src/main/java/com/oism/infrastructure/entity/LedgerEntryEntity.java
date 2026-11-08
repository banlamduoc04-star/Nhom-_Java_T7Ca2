package com.oism.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "ledger_entries",
    indexes = {
        @Index(name = "idx_tenant_account_created", columnList = "tenantId, accountId, createdAt DESC, id DESC")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 64)
    private String accountId;

    @Column(precision = 18, scale = 4, nullable = false)
    private BigDecimal debitAmount;

    @Column(precision = 18, scale = 4, nullable = false)
    private BigDecimal creditAmount;

    @Column(precision = 18, scale = 4, nullable = false)
    private BigDecimal balanceAfter;

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;
}