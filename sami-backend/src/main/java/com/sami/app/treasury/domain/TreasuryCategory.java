package com.sami.app.treasury.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "treasury_categories") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TreasuryCategory extends BaseEntity {
    public enum Kind { INCOME, EXPENSE }
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private Kind kind;
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 255) private String description;
    @Builder.Default @Column(nullable = false) private boolean active = true;
    @Column(name = "display_order", nullable = false) private int displayOrder;
}
