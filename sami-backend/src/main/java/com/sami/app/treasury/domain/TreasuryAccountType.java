package com.sami.app.treasury.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/** Configurable type of a treasury account (cash, bank, POS, gateway, ...). */
@Entity @Table(name = "treasury_account_types") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TreasuryAccountType extends BaseEntity {
    @Column(name = "tenant_id") private Long tenantId;
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 255) private String description;
    @Column(name = "requires_bank_details", nullable = false) private boolean requiresBankDetails;
    @Column(name = "allows_negative_balance", nullable = false) private boolean allowsNegativeBalance;
    @Builder.Default @Column(nullable = false) private boolean active = true;
    @Column(name = "is_system", nullable = false) private boolean isSystem;
    @Column(name = "display_order", nullable = false) private int displayOrder;
}
