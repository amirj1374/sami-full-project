package com.sami.app.purchasing.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A configurable purchase cancellation reason. */
@Entity
@Table(name = "pur_cancel_reasons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurCancelReason extends BaseEntity {

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
