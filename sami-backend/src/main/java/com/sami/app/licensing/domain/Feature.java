package com.sami.app.licensing.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * A licensable capability — the unit business modules gate on via
 * {@code @PreAuthorize("@features.enabled('inventory.advanced')")}. Adding a
 * feature is a row plus a gate annotation; the licensing core never changes.
 *
 * <p>{@code isCore} features stay available under the {@code limited} expiry
 * behaviour; {@code dependencies} are validated so a feature can't be granted
 * without its prerequisites.
 */
@Entity
@Table(name = "features")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feature extends BaseEntity {

    @Column(nullable = false, unique = true, length = 128)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "module_code", length = 64)
    private String moduleCode;

    @Column(name = "license_required", nullable = false)
    @Builder.Default
    private boolean licenseRequired = true;

    @Column(name = "is_core", nullable = false)
    private boolean isCore;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /** Lifecycle state (enabled / trial / beta / premium / deprecated / hidden). */
    @jakarta.persistence.ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "state_id", nullable = false)
    private FeatureState state;

    /** Trial window length, used when the state is a trial state. */
    @Column(name = "trial_days", nullable = false)
    @Builder.Default
    private int trialDays = 0;

    /** Prerequisite features that must also be granted. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "feature_dependencies",
            joinColumns = @JoinColumn(name = "feature_id"),
            inverseJoinColumns = @JoinColumn(name = "depends_on_id"))
    @Builder.Default
    private Set<Feature> dependencies = new HashSet<>();
}
