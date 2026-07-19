package com.sami.app.knowledge.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
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
 * One step of a procedure.
 *
 * <p>Steps sharing a {@code parallelGroup} may be performed in any order.
 * A decision step uses {@code branchConfig} to map an outcome to the step number
 * to continue from, e.g. {@code {"yes": 4, "no": 7}} — branching is data, so a
 * procedure's shape changes without code.
 */
@Entity
@Table(name = "kb_sop_steps")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SopStep extends BaseEntity {

    @Column(name = "sop_id", nullable = false)
    private Long sopId;

    @Column(name = "parent_step_id")
    private Long parentStepId;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "step_type_id", nullable = false)
    private KbStepType stepType;

    @Column(nullable = false, length = 255) private String title;
    @Column(columnDefinition = "text") private String instruction;
    @Column(name = "expected_result", length = 2000) private String expectedResult;

    @Column(name = "is_mandatory", nullable = false)
    @Builder.Default
    private boolean isMandatory = true;

    @Column(name = "parallel_group", length = 64) private String parallelGroup;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "condition_config")
    @Builder.Default
    private Map<String, Object> conditionConfig = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "branch_config")
    @Builder.Default
    private Map<String, Object> branchConfig = new HashMap<>();

    @Column(name = "estimated_minutes") private Integer estimatedMinutes;
    @Column(length = 1000) private String warning;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
}
