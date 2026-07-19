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
import java.util.ArrayList;
import java.util.List;

/**
 * A custom field definition. Carries its own validation constraints, the
 * capability flags the spec requires (searchable / sortable / reportable /
 * localized), per-field permissions, and an optional link to a Data Quality rule
 * so deep validation stays centralised in that module rather than duplicated here.
 */
@Entity
@Table(name = "meta_fields")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetaField extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entity_id", nullable = false)
    private MetaEntity entity;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 160)
    private String label;

    @Column(name = "help_text", length = 1000)
    private String helpText;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_type_id", nullable = false)
    private MetaFieldType fieldType;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "default_value", length = 2000)
    private String defaultValue;

    @Column(name = "min_value", precision = 18, scale = 4)
    private BigDecimal minValue;

    @Column(name = "max_value", precision = 18, scale = 4)
    private BigDecimal maxValue;

    @Column(name = "min_length")
    private Integer minLength;

    @Column(name = "max_length")
    private Integer maxLength;

    @Column(length = 500)
    private String pattern;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    @Builder.Default
    private List<Object> options = new ArrayList<>();

    @Column(name = "reference_entity", length = 128)
    private String referenceEntity;

    @Column(nullable = false)
    @Builder.Default
    private boolean searchable = true;

    @Column(nullable = false)
    private boolean sortable;

    @Column(nullable = false)
    @Builder.Default
    private boolean reportable = true;

    @Column(nullable = false)
    private boolean localized;

    @Column(name = "view_permission", length = 128)
    private String viewPermission;

    @Column(name = "edit_permission", length = 128)
    private String editPermission;

    /** Optional Data Quality rule code applied on top of type validation. */
    @Column(name = "quality_rule_code", length = 64)
    private String qualityRuleCode;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_by_email", length = 255)
    private String createdByEmail;
}
