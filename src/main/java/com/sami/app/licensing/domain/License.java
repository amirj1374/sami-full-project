package com.sami.app.licensing.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A licence issued to a tenant (optionally narrowed to one company — otherwise
 * every company under the tenant shares it). Grants the plan's features and
 * limits, with per-licence overrides so entitlements change without a deployment.
 */
@Entity
@Table(name = "licenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class License extends BaseEntity {

    @Column(name = "license_key", nullable = false, unique = true, length = 255)
    private String licenseKey;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "license_type_id", nullable = false)
    private LicenseType licenseType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** Null means the licence is shared by all companies in the tenant. */
    @Column(name = "company_id")
    private Long companyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private LicensingStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expiry_behavior_id")
    private ExpiryBehavior expiryBehavior;

    @Column(name = "activation_date")
    private Instant activationDate;

    @Column(name = "expiration_date")
    private Instant expirationDate;

    @Column(name = "grace_days", nullable = false)
    @Builder.Default
    private int graceDays = 0;

    @Column(name = "activated_at")
    private Instant activatedAt;

    /** Binds an activation to one installation; blocks duplicate activation. */
    @Column(name = "activation_fingerprint", length = 255)
    private String activationFingerprint;

    /** Per-licence limit overrides keyed by limit-type code. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "limit_overrides", nullable = false)
    @Builder.Default
    private Map<String, Object> limitOverrides = new HashMap<>();

    @Column(length = 2000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_status_id")
    private PaymentStatus paymentStatus;

    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew;

    /** MANUAL | ONLINE | OFFLINE | EMERGENCY — how this licence was activated. */
    @Column(name = "activation_mode", nullable = false, length = 32)
    @Builder.Default
    private String activationMode = "MANUAL";

    /** Emergency activation grants access until this moment, then lapses. */
    @Column(name = "emergency_until")
    private Instant emergencyUntil;

    @Column(name = "transferred_at")
    private Instant transferredAt;

    @Column(name = "transfer_count", nullable = false)
    @Builder.Default
    private int transferCount = 0;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_by_email", length = 255)
    private String createdByEmail;

    @OneToMany(mappedBy = "license", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LicenseFeature> featureOverrides = new ArrayList<>();

    /** True while the licence is inside its paid term. */
    public boolean withinTerm(Instant now) {
        return expirationDate == null || expirationDate.isAfter(now);
    }

    /** True while an emergency activation window is still open. */
    public boolean withinEmergency(Instant now) {
        return emergencyUntil != null && emergencyUntil.isAfter(now);
    }

    /** True while expired but still inside the configured grace window. */
    public boolean withinGrace(Instant now) {
        return expirationDate != null
                && !expirationDate.isAfter(now)
                && graceDays > 0
                && expirationDate.plusSeconds((long) graceDays * 86_400L).isAfter(now);
    }
}
