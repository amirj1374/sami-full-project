package com.sami.app.files.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Configurable retention. {@code retainDays == null} means permanent. */
@Entity
@Table(name = "retention_policies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RetentionPolicy extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "retain_days")
    private Integer retainDays;

    /** Resolves a {@code RetentionHandler} bean. */
    @Column(name = "action_on_expiry", nullable = false, length = 64)
    private String actionOnExpiry;

    @Column(name = "allows_legal_hold", nullable = false)
    private boolean allowsLegalHold;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "tenant_id")
    private Long tenantId;

    public boolean isPermanent() {
        return retainDays == null;
    }
}
