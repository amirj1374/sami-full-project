package com.sami.app.knowledge.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** One approval decision on one article version, at one stage of the chain. */
@Entity
@Table(name = "kb_approvals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KbApproval extends BaseEntity {

    @Column(name = "article_version_id", nullable = false) private Long articleVersionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    private KbApprovalStage stage;

    /** pending | approved | rejected | skipped */
    @Column(nullable = false, length = 32) @Builder.Default private String decision = "pending";

    @Column(name = "approver_id") private Long approverId;
    @Column(name = "approver_email", length = 255) private String approverEmail;
    @Column(name = "decided_at") private Instant decidedAt;
    @Column(length = 2000) private String comment;
    /** Reference to an electronic signature, when the stage requires one. */
    @Column(name = "signature_ref", length = 255) private String signatureRef;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;

    public boolean isDecided() {
        return !"pending".equals(decision);
    }

    public boolean isApproved() {
        return "approved".equals(decision) || "skipped".equals(decision);
    }
}
