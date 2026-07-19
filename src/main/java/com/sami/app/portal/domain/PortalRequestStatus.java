package com.sami.app.portal.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Configurable request lifecycle. */
@Entity @Table(name = "portal_request_statuses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalRequestStatus extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_open_state", nullable = false) private boolean isOpenState;
    @Column(name = "awaits_customer", nullable = false) private boolean awaitsCustomer;
    @Column(name = "is_resolved_state", nullable = false) private boolean isResolvedState;
    @Column(name = "is_closed_state", nullable = false) private boolean isClosedState;
    @Column(name = "allows_reply", nullable = false) private boolean allowsReply;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
