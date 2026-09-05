package com.sami.app.treasury.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name = "treasury_daily_closings") @Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class TreasuryDailyClosing extends BaseEntity {
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "account_id", nullable = false) private TreasuryAccount account;
    @Column(name = "closing_date", nullable = false) private LocalDate closingDate;
    @Column(name = "expected_balance", nullable = false, precision = 18, scale = 2) private BigDecimal expectedBalance;
    @Column(name = "declared_balance", nullable = false, precision = 18, scale = 2) private BigDecimal declaredBalance;
    @Column(name = "difference_amount", nullable = false, precision = 18, scale = 2) private BigDecimal differenceAmount;
    @Column(length = 1000) private String note;
    @Column(name = "closed_by") private Long closedBy;
    @Column(name = "closed_at", nullable = false) @Builder.Default private Instant closedAt = Instant.now();
}
