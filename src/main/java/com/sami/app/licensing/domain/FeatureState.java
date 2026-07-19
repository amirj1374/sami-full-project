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

/**
 * Configurable feature lifecycle state (enabled, disabled, trial, beta, premium,
 * deprecated, hidden). Behaviour travels with the flags, so a new state is a row
 * and the gate logic never changes.
 */
@Entity
@Table(name = "feature_states")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeatureState extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    /** The gate may pass only when this is true. */
    @Column(name = "grants_access", nullable = false)
    private boolean grantsAccess;

    /** Beta: the tenant must opt in via its configuration. */
    @Column(name = "requires_optin", nullable = false)
    private boolean requiresOptin;

    /** Trial: access is bounded by the feature's trialDays from activation. */
    @Column(name = "is_trial", nullable = false)
    private boolean isTrial;

    @Column(name = "is_premium", nullable = false)
    private boolean isPremium;

    @Column(name = "is_deprecated", nullable = false)
    private boolean isDeprecated;

    @Column(name = "is_hidden", nullable = false)
    private boolean isHidden;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
