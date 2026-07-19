package com.sami.app.portal.domain;

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

/** Portal audit trail. Records device and IP on every action, per the spec. */
@Entity @Table(name = "portal_audit_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalAuditLog extends BaseEntity {
    @Column(name = "account_id") private Long accountId;
    @Column(name = "entity_type", nullable = false, length = 64) private String entityType;
    @Column(name = "entity_id") private Long entityId;
    @Column(nullable = false, length = 64) private String action;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "old_values") private Map<String, Object> oldValues;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "new_values") private Map<String, Object> newValues;
    @Column(name = "actor_kind", nullable = false, length = 16) @Builder.Default
    private String actorKind = "CUSTOMER";
    @Column(name = "actor_id") private Long actorId;
    @Column(name = "actor_label", length = 255) private String actorLabel;
    @Column(name = "ip_address", length = 64) private String ipAddress;
    @Column(name = "device_label", length = 255) private String deviceLabel;
    @Column(name = "user_agent", length = 500) private String userAgent;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
}
