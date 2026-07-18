package com.sami.app.authz.domain;

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
}
