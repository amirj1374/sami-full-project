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

/** Per-locale label/help for a custom field, so extensions localise like the core UI. */
@Entity
@Table(name = "meta_field_translations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MetaFieldTranslation extends BaseEntity {

    @Column(name = "field_id", nullable = false)
    private Long fieldId;

    @Column(nullable = false, length = 16)
    private String locale;

    @Column(nullable = false, length = 160)
    private String label;

    @Column(name = "help_text", length = 1000)
    private String helpText;
}
