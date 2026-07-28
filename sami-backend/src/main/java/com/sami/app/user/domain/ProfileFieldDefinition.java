package com.sami.app.user.domain;

import com.sami.app.common.domain.BaseEntity;
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
 * Runtime definition of a custom profile field: its key into
 * {@link UserProfile#getCustomFields()}, its type and its validation rules.
 * Administrators create these without any code or schema change.
 */
@Entity
@Table(name = "profile_field_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileFieldDefinition extends BaseEntity {

    /** Value types a custom field can hold; validation dispatches on this. */
    public enum FieldType { TEXT, NUMBER, DATE, BOOLEAN, SELECT }

    @Column(name = "field_key", nullable = false, unique = true, length = 64)
    private String fieldKey;

    @Column(nullable = false, length = 100)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 16)
    private FieldType fieldType;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "min_length")
    private Integer minLength;

    @Column(name = "max_length")
    private Integer maxLength;

    /** Optional regex the (string) value must match. */
    @Column(length = 255)
    private String pattern;

    /** Allowed values for SELECT fields. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options")
    private List<String> options;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
