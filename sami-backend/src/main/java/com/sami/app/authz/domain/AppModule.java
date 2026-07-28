package com.sami.app.authz.domain;

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

/**
 * A functional area of the application (e.g. Users, Products).
 *
 * <p>{@code code} is the lowercase slug used as the permission-code prefix and is
 * immutable once created. Modules drive the navigation menu ({@code icon},
 * {@code path}, {@code displayOrder}, {@code enabled}) so a module created at
 * runtime instantly gets a menu entry and its own permission namespace.
 *
 * <p>Named {@code AppModule} to avoid shadowing {@link java.lang.Module}; the
 * JSON/API wording remains "module".
 */
@Entity
@Table(name = "modules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppModule extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    /** mdi icon name rendered in the navigation drawer, e.g. {@code mdi-cog}. */
    @Column(length = 64)
    private String icon;

    /** Frontend route path, e.g. {@code /products}. */
    @Column(length = 255)
    private String path;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    // ----------------------------------------------------------------
    // Lifecycle (V25)
    //
    // Backend and frontend readiness are tracked independently because
    // they genuinely move apart: most modules here have a complete
    // server side and no screens. Inferring one from the other — which
    // is what the frontend used to do by checking its own router — is
    // wrong in both directions.
    // ----------------------------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "backend_status_id", nullable = false)
    private ModuleStatus backendStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "frontend_status_id", nullable = false)
    private ModuleStatus frontendStatus;

    /**
     * Explicit override. NULL means the overall status is derived from the
     * two axes by {@code ModuleLifecycle}; setting it pins the answer.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "overall_status_id")
    private ModuleStatus overallStatus;

    /** Version in which the module shipped, or is expected to. */
    @Column(name = "release_version", length = 32)
    private String releaseVersion;

    @Column(name = "progress_percentage", nullable = false)
    private short progressPercentage;

    @Column(name = "development_notes", length = 2000)
    private String developmentNotes;

    /**
     * Distinct from {@link #enabled}: an administrator switches a module off
     * for this installation, whereas availability records whether it is fit
     * to be used at all. Keeping them apart means disabling a module locally
     * does not rewrite its lifecycle record.
     */
    @Column(name = "is_available", nullable = false)
    private boolean isAvailable;

    @Column(name = "is_production_ready", nullable = false)
    private boolean isProductionReady;
}
