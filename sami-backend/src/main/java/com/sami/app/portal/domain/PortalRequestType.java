package com.sami.app.portal.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Configurable self-service request type. */
@Entity @Table(name = "portal_request_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalRequestType extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 120) private String name;
    @Column(length = 500) private String description;
    @Column(name = "requires_attachment", nullable = false) private boolean requiresAttachment;
    @Column(name = "allows_attachment", nullable = false) private boolean allowsAttachment;
    @Column(name = "sla_hours") private Integer slaHours;
    /** The module that will ultimately handle it, when that module exists. */
    @Column(name = "target_module", length = 64) private String targetModule;
    @Column(nullable = false) private boolean enabled;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
