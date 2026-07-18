package com.sami.app.crm.domain;

import com.sami.app.common.domain.BaseEntity;
import com.sami.app.common.fields.DynamicFieldSpec;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * Runtime definition of one configurable customer-preference field (favorite
 * brand, budget range, communication preference…). Values live in
 * {@code customers.preferences} (JSONB); validation goes through the shared
 * {@code DynamicFieldValidator}, so new preferences never require code changes.
 */
@Entity
@Table(name = "crm_preference_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenceDefinition extends BaseEntity {

    @Column(name = "pref_key", nullable = false, unique = true, length = 64)
    private String prefKey;

    @Column(nullable = false, length = 100)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 16)
    private DynamicFieldSpec.Type fieldType;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "min_length")
    private Integer minLength;

    @Column(name = "max_length")
    private Integer maxLength;

    @Column(length = 255)
    private String pattern;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options")
    private List<String> options;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public DynamicFieldSpec toSpec() {
        return new DynamicFieldSpec(prefKey, label, fieldType, required,
                minLength, maxLength, pattern, options);
    }
}
