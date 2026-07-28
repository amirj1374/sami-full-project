package com.sami.app.purchasing.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * An amount-based approval rule: submitted purchases whose total reaches any
 * active {@code minAmount} require approval; below every threshold they
 * auto-approve. Multi-level / department rules extend this table later.
 */
@Entity
@Table(name = "pur_approval_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurApprovalRule extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "min_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal minAmount;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
