package com.sami.app.automation.domain;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * A failed execution parked for retry / manual review. The retry scheduler
 * (future phase) drains rows where {@code resolved = false} and
 * {@code nextRetryAt <= now}.
 */
@Entity
@Table(name = "automation_failures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutomationFailure extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private AutomationRule rule;

    @Column(name = "execution_id")
    private Long executionId;

    @Column(length = 2000)
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, Object> payload;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(nullable = false)
    private boolean resolved;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
