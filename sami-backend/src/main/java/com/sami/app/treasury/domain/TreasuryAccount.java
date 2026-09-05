package com.sami.app.treasury.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/** One operational account model avoids duplicate owners for bank accounts and cash boxes. */
@Entity @Table(name = "treasury_accounts") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TreasuryAccount extends BaseEntity {
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "company_id") private Long companyId;
    @Column(name = "branch_id") private Long branchId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "account_type_id", nullable = false) private TreasuryAccountType accountType;
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 160) private String name;
    @Column(name = "currency_code", nullable = false, length = 3) @Builder.Default private String currencyCode = "IRR";
    @Column(name = "opening_balance", nullable = false, precision = 18, scale = 2) @Builder.Default private BigDecimal openingBalance = BigDecimal.ZERO;
    @Column(name = "current_balance", nullable = false, precision = 18, scale = 2) @Builder.Default private BigDecimal currentBalance = BigDecimal.ZERO;
    @Column(name = "allow_negative_balance", nullable = false) private boolean allowNegativeBalance;
    @Column(name = "responsible_user_id") private Long responsibleUserId;
    @Column(name = "bank_name", length = 160) private String bankName;
    @Column(name = "bank_branch", length = 160) private String bankBranch;
    @Column(length = 64) private String iban;
    @Column(name = "account_number", length = 80) private String accountNumber;
    @Column(name = "card_number", length = 32) private String cardNumber;
    @Column(name = "account_holder", length = 160) private String accountHolder;
    @Column(length = 1000) private String description;
    @Builder.Default @Column(nullable = false) private boolean active = true;
}
