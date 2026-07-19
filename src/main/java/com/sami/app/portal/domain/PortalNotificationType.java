package com.sami.app.portal.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Configurable notification type. Security alerts cannot be opted out of. */
@Entity @Table(name = "portal_notification_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalNotificationType extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 120) private String name;
    @Column(name = "is_marketing", nullable = false) private boolean isMarketing;
    @Column(name = "is_security", nullable = false) private boolean isSecurity;
    @Column(name = "opt_out_allowed", nullable = false) private boolean optOutAllowed;
    @Column(nullable = false) private boolean enabled;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
