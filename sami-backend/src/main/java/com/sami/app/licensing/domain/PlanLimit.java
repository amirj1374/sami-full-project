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

/** A plan's ceiling for one limit type. {@code -1} means unlimited. */
@Entity
@Table(name = "plan_limits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanLimit extends BaseEntity {

    public static final long UNLIMITED = -1L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "limit_type_id", nullable = false)
    private UsageLimitType limitType;

    @Column(name = "limit_value", nullable = false)
    private long limitValue;
}
