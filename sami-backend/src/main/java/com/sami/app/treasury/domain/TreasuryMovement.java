package com.sami.app.treasury.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Immutable, account-side financial evidence. Positive is inflow; negative is outflow. */
@Entity @Table(name = "treasury_movements") @Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class TreasuryMovement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "transaction_id", nullable = false) private TreasuryTransaction transaction;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "account_id", nullable = false) private TreasuryAccount account;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal amount;
    @Column(name = "balance_after", nullable = false, precision = 18, scale = 2) private BigDecimal balanceAfter;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "created_at", nullable = false, updatable = false) @Builder.Default private Instant createdAt = Instant.now();
}
