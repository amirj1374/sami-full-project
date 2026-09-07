package com.sami.app.sales.delivery;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;

@Entity @Table(name="sales_delivery_audit_history") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DeliveryAudit extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="delivery_id",nullable=false) Delivery delivery;
    @Column(name="tenant_id",nullable=false) Long tenantId; @Column(nullable=false) String action; @Column(name="actor_id") Long actorId; @Column(name="actor_email") String actorEmail;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="old_value") Map<String,Object> oldValue; @JdbcTypeCode(SqlTypes.JSON) @Column(name="new_value") Map<String,Object> newValue;
    @Column(name="occurred_at",nullable=false) @Builder.Default Instant occurredAt=Instant.now();
}
