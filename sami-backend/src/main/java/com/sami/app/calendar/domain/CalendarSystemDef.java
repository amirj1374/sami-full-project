package com.sami.app.calendar.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A chronology the installation understands. Named {@code ...Def} to avoid
 * colliding with the {@code CalendarSystem} SPI interface it points at through
 * {@code handlerKey}.
 */
@Entity
@Table(name = "calendar_systems")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CalendarSystemDef extends BaseEntity {

    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;

    /** Resolves to a {@code CalendarSystem} bean via the registry. */
    @Column(name = "handler_key", nullable = false, length = 64) private String handlerKey;

    @Column(name = "months_in_year", nullable = false) private short monthsInYear;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "is_active", nullable = false) private boolean isActive;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
