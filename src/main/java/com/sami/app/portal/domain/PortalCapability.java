package com.sami.app.portal.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A customer-facing capability. Deliberately separate from the staff
 * {@code permissions} table so the two vocabularies cannot drift together.
 */
@Entity @Table(name = "portal_capabilities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalCapability extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 120) private String name;
    @Column(length = 500) private String description;
    @Column(name = "granted_by_default", nullable = false) private boolean grantedByDefault;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
