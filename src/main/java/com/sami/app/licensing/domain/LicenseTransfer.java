package com.sami.app.licensing.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Append-only record of a licence moving between tenants/installations. */
@Entity
@Table(name = "license_transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LicenseTransfer extends BaseEntity {

    @Column(name = "license_id", nullable = false)
    private Long licenseId;

    @Column(name = "from_tenant_id")
    private Long fromTenantId;

    @Column(name = "to_tenant_id", nullable = false)
    private Long toTenantId;

    @Column(name = "from_fingerprint", length = 255)
    private String fromFingerprint;

    @Column(length = 1000)
    private String reason;

    @Column(name = "transferred_by")
    private Long transferredBy;

    @Column(name = "transferred_by_email", length = 255)
    private String transferredByEmail;

    @Column(name = "transferred_at", nullable = false)
    @Builder.Default
    private Instant transferredAt = Instant.now();
}
