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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The structured face of an article VERSION — not of the article.
 *
 * <p>Attaching the SOP to a version means its steps are versioned with the
 * content, so a published procedure cannot change underneath the people
 * following it.
 */
@Entity
@Table(name = "kb_sops")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Sop extends BaseEntity {

    @Column(name = "article_version_id", nullable = false)
    private Long articleVersionId;

    @Column(name = "sop_number", nullable = false, length = 32)
    private String sopNumber;

    @Column(length = 2000) private String purpose;
    @Column(length = 2000) private String scope;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_inputs", nullable = false)
    @Builder.Default
    private List<String> requiredInputs = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_outputs", nullable = false)
    @Builder.Default
    private List<String> requiredOutputs = new ArrayList<>();

    @Column(name = "estimated_minutes") private Integer estimatedMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_level_id")
    private KbRiskLevel riskLevel;

    @Column(name = "effective_date") private LocalDate effectiveDate;
    @Column(name = "review_date") private LocalDate reviewDate;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
}
