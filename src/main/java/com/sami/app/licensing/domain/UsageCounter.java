package com.sami.app.licensing.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Recorded usage for one tenant/limit/period. {@code periodKey} is empty for
 * lifetime counters (e.g. user seats) or a period stamp such as {@code 2026-07}
 * for windowed metrics (API calls) — enabling future usage-based billing.
 */
@Entity
@Table(name = "usage_counters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageCounter extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "limit_type_id", nullable = false)
    private UsageLimitType limitType;

    @Column(name = "period_key", nullable = false, length = 32)
    @Builder.Default
    private String periodKey = "";

    @Column(name = "current_value", nullable = false)
    @Builder.Default
    private long currentValue = 0;

    @Column(name = "recorded_at", nullable = false)
    @Builder.Default
    private Instant recordedAt = Instant.now();
}
