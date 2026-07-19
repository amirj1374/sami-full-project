package com.sami.app.portal.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

/**
 * A per-device session. Individually revocable, so locking an account kills live
 * sessions rather than waiting for token expiry.
 */
@Entity @Table(name = "portal_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalSession extends BaseEntity {
    @Column(name = "account_id", nullable = false) private Long accountId;
    @Column(name = "token_hash", nullable = false, length = 128) private String tokenHash;
    @Column(name = "device_label", length = 160) private String deviceLabel;
    @Column(name = "device_fingerprint", length = 255) private String deviceFingerprint;
    @Column(name = "ip_address", length = 64) private String ipAddress;
    @Column(name = "user_agent", length = 500) private String userAgent;
    @Column(name = "is_trusted", nullable = false) private boolean isTrusted;
    @Column(name = "issued_at", nullable = false) @Builder.Default private Instant issuedAt = Instant.now();
    @Column(name = "last_seen_at") private Instant lastSeenAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "revoked_reason", length = 255) private String revokedReason;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;

    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }
}
