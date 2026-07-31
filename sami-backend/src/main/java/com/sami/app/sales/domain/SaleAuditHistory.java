package com.sami.app.sales.domain;
import com.sami.app.common.domain.BaseEntity; import jakarta.persistence.*; import lombok.*; import org.hibernate.annotations.JdbcTypeCode; import org.hibernate.type.SqlTypes; import java.time.Instant; import java.util.Map;
@Entity @Table(name="sale_audit_history") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SaleAuditHistory extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="sale_id") private Sale sale; @Column(name="tenant_id",nullable=false) private Long tenantId;
 @Column(nullable=false) private String action; @Column(name="actor_id") private Long actorId; @Column(name="actor_email") private String actorEmail;
 @JdbcTypeCode(SqlTypes.JSON) @Column(name="old_value") private Map<String,Object> oldValue; @JdbcTypeCode(SqlTypes.JSON) @Column(name="new_value") private Map<String,Object> newValue;
 @Column(name="ip_address") private String ipAddress; @Column private String device; @Column(name="occurred_at",nullable=false) @Builder.Default private Instant occurredAt=Instant.now();
}
