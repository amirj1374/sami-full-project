package com.sami.app.portal.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

/** A self-service request raised by a customer. */
@Entity @Table(name = "portal_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalRequest extends BaseEntity {
    @Column(name = "request_number", nullable = false, length = 32) private String requestNumber;
    @Column(name = "account_id", nullable = false) private Long accountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false)
    private PortalRequestType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private PortalRequestStatus status;

    @Column(nullable = false, length = 255) private String subject;
    @Column(columnDefinition = "text") private String body;
    @Column(name = "related_module", length = 64) private String relatedModule;
    @Column(name = "related_entity", length = 64) private String relatedEntity;
    @Column(name = "related_record_id") private Long relatedRecordId;
    @Column(name = "assigned_to") private Long assignedTo;
    @Column(name = "sla_due_at") private Instant slaDueAt;
    @Column(name = "resolved_at") private Instant resolvedAt;
    @Column(name = "resolved_by") private Long resolvedBy;
    @Column(length = 2000) private String resolution;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
}
