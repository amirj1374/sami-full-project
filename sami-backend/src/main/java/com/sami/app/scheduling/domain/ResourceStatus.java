package com.sami.app.scheduling.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Resource availability state.
 *
 * <p>{@code allowsBooking} is separate from {@code isAvailable} so a resource
 * in Maintenance keeps the reservations already made against it while refusing
 * new ones — the "resource goes out of service" edge case.
 */
@Entity @Table(name = "resource_statuses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResourceStatus extends BaseEntity {
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 16) private String color;
    @Column(name = "allows_booking", nullable = false) private boolean allowsBooking;
    @Column(name = "is_available", nullable = false) private boolean isAvailable;
    @Column(name = "is_default", nullable = false) private boolean isDefault;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "tenant_id") private Long tenantId;
}
