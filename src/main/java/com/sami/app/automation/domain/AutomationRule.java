package com.sami.app.automation.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The unit of automation configuration.
 *
 * <p>Behaviour lives in data, never in code: {@code triggerType} is a canonical
 * key (e.g. {@code crm.customer.CREATED} or a wildcard {@code crm.customer.*}),
 * and {@code triggerConfig}/{@code conditionConfig}/{@code executionPolicy} are
 * open JSON so new capabilities never require a schema change. Ordered
 * {@link AutomationAction} steps resolve to {@code ActionProvider} beans.
 */
@Entity
@Table(name = "automation_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutomationRule extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(length = 64)
    private String category;

    @Column(nullable = false)
    @Builder.Default
    private int priority = 100;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "branch_id")
    private Long branchId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private AutomationStatus status;

    @Column(name = "trigger_type", nullable = false, length = 128)
    private String triggerType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigger_config", nullable = false)
    @Builder.Default
    private Map<String, Object> triggerConfig = new HashMap<>();

    /** Condition tree evaluated by {@code ConditionEvaluator} (AND/OR/NOT/leaf). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "condition_config", nullable = false)
    @Builder.Default
    private Map<String, Object> conditionConfig = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_policy", nullable = false)
    @Builder.Default
    private Map<String, Object> executionPolicy = new HashMap<>();

    /** Circular/recursive execution is refused unless this is explicitly enabled. */
    @Column(name = "allow_recursion", nullable = false)
    private boolean allowRecursion;

    /** Optional lifetime cap on executions; null means unlimited. */
    @Column(name = "max_executions")
    private Integer maxExecutions;

    @Column(name = "execution_count", nullable = false)
    @Builder.Default
    private long executionCount = 0;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_by_email", length = 255)
    private String createdByEmail;

    @OneToMany(mappedBy = "rule", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC, id ASC")
    @Builder.Default
    private List<AutomationAction> actions = new ArrayList<>();
}
