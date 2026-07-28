package com.sami.app.scheduling.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A reminder delivery channel.
 *
 * <p>Seeded inactive: no notification module exists yet. The catalogue is
 * complete so that activating SMS later is a configuration flip, not a release.
 */
@Entity @Table(name = "reminder_channels")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReminderChannel extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(name = "handler_key", nullable = false, length = 64) private String handlerKey;
    @Column(name = "requires_phone", nullable = false) private boolean requiresPhone;
    @Column(name = "requires_email", nullable = false) private boolean requiresEmail;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "is_active", nullable = false) private boolean isActive;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
