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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * A layout variant of a published form version, selected at render time by
 * role / company / branch / device / workflow stage. Highest priority match wins.
 */
@Entity
@Table(name = "meta_form_layouts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MetaFormLayout extends BaseEntity {

    public enum TargetType { DEFAULT, ROLE, COMPANY, BRANCH, DEVICE, STAGE }

    @Column(name = "form_version_id", nullable = false)
    private Long formVersionId;

    @Column(name = "target_type", nullable = false, length = 32)
    @Builder.Default
    private String targetType = TargetType.DEFAULT.name();

    @Column(name = "target_value", length = 128)
    private String targetValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layout_json", nullable = false)
    @Builder.Default
    private Map<String, Object> layoutJson = new HashMap<>();

    @Column(nullable = false)
    @Builder.Default
    private int priority = 100;
}
