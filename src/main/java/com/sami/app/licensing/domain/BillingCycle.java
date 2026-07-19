package com.sami.app.licensing.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Configurable billing cycle; {@code months} drives renewal arithmetic. */
@Entity
@Table(name = "billing_cycles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillingCycle extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private int months = 12;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
