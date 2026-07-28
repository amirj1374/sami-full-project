package com.sami.app.metadata.domain;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * One custom-field value for one record. Typed columns (rather than a single
 * JSON blob) are what make custom fields searchable, filterable, sortable and
 * reportable — the storage kind of the field's type selects the column.
 */
@Entity
@Table(name = "meta_field_values")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetaFieldValue extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_id", nullable = false)
    private MetaField field;

    @Column(name = "module_code", nullable = false, length = 64)
    private String moduleCode;

    @Column(name = "entity_code", nullable = false, length = 64)
    private String entityCode;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(name = "value_text", length = 4000)
    private String valueText;

    @Column(name = "value_number", precision = 24, scale = 6)
    private BigDecimal valueNumber;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    @Column(name = "value_date")
    private Instant valueDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value_json")
    private Map<String, Object> valueJson;

    @Column(name = "tenant_id")
    private Long tenantId;
}
