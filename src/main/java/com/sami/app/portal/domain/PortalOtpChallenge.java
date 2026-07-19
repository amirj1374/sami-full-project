package com.sami.app.portal.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

/** A one-time code challenge. The code is stored hashed. */
@Entity @Table(name = "portal_otp_challenges")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalOtpChallenge extends BaseEntity {
    @Column(name = "account_id", nullable = false) private Long accountId;
    @Column(nullable = false, length = 64) private String purpose;
    @Column(name = "code_hash", nullable = false, length = 128) private String codeHash;
    @Column(name = "delivery_key", nullable = false, length = 64) private String deliveryKey;
    @Column(name = "delivery_target", nullable = false, length = 255) private String deliveryTarget;
    @Column(nullable = false) @Builder.Default private int attempts = 0;
    @Column(name = "max_attempts", nullable = false) @Builder.Default private int maxAttempts = 5;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "consumed_at") private Instant consumedAt;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;

    public boolean isUsable() {
        return consumedAt == null && attempts < maxAttempts && expiresAt.isAfter(Instant.now());
    }
}
