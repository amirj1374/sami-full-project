package com.sami.app.treasury.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "treasury_cheque_statuses") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TreasuryChequeStatus extends BaseEntity {
    @Column(name = "tenant_id") private Long tenantId;
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 255) private String description;
    @Column(nullable = false) private boolean terminal;
    @Builder.Default @Column(nullable = false) private boolean active = true;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
}
