package com.sami.app.comm.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Routing decision data: which channel to prefer, where to fall back, and
 * whether delivery defers to business hours (with an emergency override
 * priority). Evaluated highest priority first; first match wins.
 */
@Entity @Table(name = "comm_routing_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommRoutingRule extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 500) private String description;
    /** NULL = every module. */
    @Column(name = "module_code", length = 64) private String moduleCode;
    @Column(name = "min_message_priority", nullable = false) private int minMessagePriority;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "preferred_channel_id", nullable = false)
    private CommChannel preferredChannel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fallback_channel_id")
    private CommChannel fallbackChannel;

    @Column(name = "respect_business_hours", nullable = false) private boolean respectBusinessHours;
    /** Messages at/above this priority ignore business hours. 0 = never. */
    @Column(name = "emergency_priority", nullable = false) private int emergencyPriority;
    @Column(name = "is_active", nullable = false) private boolean isActive;
    @Column(nullable = false) private int priority;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;

    public boolean matches(String module, int messagePriority) {
        if (!isActive) {
            return false;
        }
        if (moduleCode != null && !moduleCode.equals(module)) {
            return false;
        }
        return messagePriority >= minMessagePriority;
    }
}
