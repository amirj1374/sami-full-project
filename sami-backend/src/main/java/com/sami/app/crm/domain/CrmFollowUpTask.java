package com.sami.app.crm.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "crm_follow_up_tasks")
@Getter @Setter @NoArgsConstructor
public class CrmFollowUpTask extends BaseEntity {
    @Column(name="tenant_id", nullable=false, updatable=false) private Long tenantId;
    @Column(name="customer_id") private Long customerId;
    @Column(name="lead_id") private Long leadId;
    @Column(name="opportunity_id") private Long opportunityId;
    @Column(name="assigned_user_id") private Long assignedUserId;
    @Column(name="due_at") private Instant dueAt;
    @Column(nullable=false, length=32) private String status = "OPEN";
    @Column(length=32) private String outcome;
    @Column(name="outcome_note", length=4000) private String outcomeNote;
    @Column(name="completed_at") private Instant completedAt;
    @Column(name="idempotency_key", length=160) private String idempotencyKey;
}
