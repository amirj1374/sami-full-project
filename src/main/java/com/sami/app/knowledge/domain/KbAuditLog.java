package com.sami.app.knowledge.domain;

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

import java.util.Map;

/** Per-module audit trail, including actor IP. */
@Entity
@Table(name = "kb_audit_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KbAuditLog extends BaseEntity {

    @Column(name = "entity_type", nullable = false, length = 64) private String entityType;
    @Column(name = "entity_id") private Long entityId;
    @Column(nullable = false, length = 64) private String action;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "old_values") private Map<String, Object> oldValues;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "new_values") private Map<String, Object> newValues;
    @Column(name = "actor_id") private Long actorId;
    @Column(name = "actor_email", length = 255) private String actorEmail;
    @Column(name = "actor_ip", length = 64) private String actorIp;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
}
