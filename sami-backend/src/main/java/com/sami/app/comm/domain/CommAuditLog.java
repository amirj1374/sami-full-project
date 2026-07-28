package com.sami.app.comm.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/** Audit trail; same JSONB shape as kb_audit_log and schedule_audit_log. */
@Entity @Table(name = "comm_audit_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommAuditLog extends BaseEntity {
    @Column(name = "entity_type", nullable = false, length = 64) private String entityType;
    @Column(name = "entity_id", nullable = false) private Long entityId;
    @Column(nullable = false, length = 64) private String action;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "old_values") private Map<String, Object> oldValues;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "new_values") private Map<String, Object> newValues;
    @Column(name = "actor_id") private Long actorId;
    @Column(name = "actor_email", length = 255) private String actorEmail;
    @Column(name = "actor_ip", length = 64) private String actorIp;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
}
