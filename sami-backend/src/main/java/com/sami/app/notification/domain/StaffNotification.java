package com.sami.app.notification.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "staff_notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StaffNotification extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String type;

    @Column(name = "title_key", nullable = false, length = 160)
    private String titleKey;

    @Column(name = "message_key", nullable = false, length = 160)
    private String messageKey;

    @Column(nullable = false, length = 500)
    private String route;

    @Column(name = "idempotency_key", nullable = false, length = 220, updatable = false)
    private String idempotencyKey;

    @Column(name = "read_at")
    private Instant readAt;
}
