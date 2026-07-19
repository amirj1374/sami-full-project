package com.sami.app.portal.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

/** An in-portal notification. */
@Entity @Table(name = "portal_notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalNotification extends BaseEntity {
    @Column(name = "account_id", nullable = false) private Long accountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false)
    private PortalNotificationType type;

    @Column(nullable = false, length = 255) private String title;
    @Column(length = 2000) private String body;
    @Column(name = "related_module", length = 64) private String relatedModule;
    @Column(name = "related_record_id") private Long relatedRecordId;
    @Column(name = "read_at") private Instant readAt;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
}
