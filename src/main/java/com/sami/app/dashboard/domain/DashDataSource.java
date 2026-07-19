package com.sami.app.dashboard.domain;

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
 * Configurable data source. {@code providerKey} resolves to a
 * {@code ReportingProvider} bean; sources whose provider is not yet registered
 * are inert until a module supplies one.
 */
@Entity
@Table(name = "dash_data_sources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashDataSource extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "provider_key", nullable = false, length = 64)
    private String providerKey;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
