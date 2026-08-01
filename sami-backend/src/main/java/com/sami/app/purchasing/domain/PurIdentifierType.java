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

/**
 * A configurable identifier kind for serialized units (IMEI, IMEI2, serial,
 * barcode, internal code…). {@code satisfiesSerial}/{@code satisfiesImei} back
 * the per-item requirement flags; new identifier kinds are rows, not code.
 */
@Entity
@Table(name = "pur_identifier_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurIdentifierType extends BaseEntity {

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "satisfies_serial", nullable = false)
    private boolean satisfiesSerial;

    @Column(name = "satisfies_imei", nullable = false)
    private boolean satisfiesImei;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
