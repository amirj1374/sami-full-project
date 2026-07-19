package com.sami.app.metadata.domain;

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
 * An ERP entity that accepts custom fields. Business modules "register their
 * requirement" by inserting a row here; the metadata engine needs no knowledge
 * of their internals.
 */
@Entity
@Table(name = "meta_entities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MetaEntity extends BaseEntity {

    @Column(name = "module_code", nullable = false, length = 64)
    private String moduleCode;

    @Column(name = "entity_code", nullable = false, length = 64)
    private String entityCode;

    @Column(nullable = false, length = 160)
    private String label;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
