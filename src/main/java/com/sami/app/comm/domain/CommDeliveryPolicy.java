package com.sami.app.comm.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Delivery behaviour as data. Backoff = base * multiplier^(n-1), capped.
 * "Three patient retries" and "two OTP attempts, dead in five minutes" are
 * both rows in this table, never branches in a service.
 */
@Entity @Table(name = "comm_delivery_policies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommDeliveryPolicy extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 500) private String description;
    @Column(name = "max_attempts", nullable = false) private int maxAttempts;
    @Column(name = "retry_base_seconds", nullable = false) private int retryBaseSeconds;
    @Column(name = "retry_backoff_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal retryBackoffMultiplier;
    @Column(name = "retry_max_seconds", nullable = false) private int retryMaxSeconds;
    @Column(name = "timeout_seconds", nullable = false) private int timeoutSeconds;
    /** 0 = never expires. */
    @Column(name = "expiration_minutes", nullable = false) private int expirationMinutes;
    @Column(name = "requires_delivery_receipt", nullable = false) private boolean requiresDeliveryReceipt;
    @Column(name = "requires_read_receipt", nullable = false) private boolean requiresReadReceipt;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
