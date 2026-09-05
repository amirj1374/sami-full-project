package com.sami.app.treasury.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name = "treasury_cheques") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TreasuryCheque extends BaseEntity {
    public enum Direction { RECEIVED, ISSUED }
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private Direction direction;
    @Column(name = "cheque_number", nullable = false, length = 100) private String chequeNumber;
    @Column(name = "normalized_bank_name", nullable = false, length = 160) private String normalizedBankName;
    @Column(name = "bank_name", nullable = false, length = 160) private String bankName;
    @Column(name = "bank_branch", length = 160) private String bankBranch;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal amount;
    @Column(name = "currency_code", nullable = false, length = 3) @Builder.Default private String currencyCode = "IRR";
    @Column(name = "owner_name", length = 160) private String ownerName;
    @Column(name = "recipient_name", length = 160) private String recipientName;
    @Column(name = "issue_date") private LocalDate issueDate;
    @Column(name = "due_date") private LocalDate dueDate;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "status_id", nullable = false) private TreasuryChequeStatus status;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "treasury_account_id") private TreasuryAccount treasuryAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "transaction_id") private TreasuryTransaction transaction;
    @Column(name = "image_file_id") private Long imageFileId;
    @Column(length = 2000) private String description;
    @Column(name = "status_changed_at") private Instant statusChangedAt;
}
