package com.sami.app.purchasing.domain;

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
 * A configurable purchase type. {@code numberPrefix} feeds the document number
 * generator (PUR-2026-000001, PUR-MOB-000145, …); new types are rows, not code.
 */
@Entity
@Table(name = "pur_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurType extends BaseEntity {

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "number_prefix", nullable = false, length = 16)
    @Builder.Default
    private String numberPrefix = "PUR";

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
