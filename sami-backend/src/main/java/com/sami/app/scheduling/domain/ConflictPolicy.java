package com.sami.app.scheduling.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** How a booking conflict is handled; {@code handlerKey} resolves to a strategy bean. */
@Entity @Table(name = "conflict_policies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConflictPolicy extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 500) private String description;
    @Column(name = "handler_key", nullable = false, length = 64) private String handlerKey;
    @Column(name = "is_blocking", nullable = false) private boolean isBlocking;
    @Column(name = "suggests_alternatives", nullable = false) private boolean suggestsAlternatives;
    @Column(name = "requires_approval", nullable = false) private boolean requiresApproval;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "is_active", nullable = false) private boolean isActive;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
