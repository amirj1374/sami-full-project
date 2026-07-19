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
 * Configurable field-type catalogue. {@code handlerKey} resolves to a
 * {@code FieldTypeHandler} bean and {@code storageKind} decides which typed
 * value column is used — so a new field type is a row plus a handler bean.
 */
@Entity
@Table(name = "meta_field_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MetaFieldType extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "handler_key", nullable = false, length = 64)
    private String handlerKey;

    /** TEXT | NUMBER | BOOLEAN | DATE | JSON */
    @Column(name = "storage_kind", nullable = false, length = 16)
    private String storageKind;

    @Column(name = "supports_options", nullable = false)
    private boolean supportsOptions;

    @Column(name = "is_reference", nullable = false)
    private boolean isReference;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
