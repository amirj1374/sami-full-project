package com.sami.app.portal.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

/** An enrolled second factor or passkey. Secrets stored hashed, never plain. */
@Entity @Table(name = "portal_credentials")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalCredential extends BaseEntity {
    @Column(name = "account_id", nullable = false) private Long accountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "method_id", nullable = false)
    private PortalAuthMethod method;

    @Column(name = "secret_hash", length = 512) private String secretHash;
    @Column(length = 160) private String label;
    @Column(name = "enrolled_at", nullable = false) @Builder.Default private Instant enrolledAt = Instant.now();
    @Column(name = "last_used_at") private Instant lastUsedAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
}
