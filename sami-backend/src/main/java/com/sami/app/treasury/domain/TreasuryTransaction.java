package com.sami.app.treasury.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Lifecycle document. Financial effects are represented by immutable TreasuryMovement rows on completion. */
@Entity @Table(name = "treasury_transactions") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TreasuryTransaction extends BaseEntity {
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "transaction_number", nullable = false, length = 64) private String transactionNumber;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "transaction_type_id", nullable = false) private TreasuryTransactionType transactionType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "status_id", nullable = false) private TreasuryTransactionStatus status;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "category_id") private TreasuryCategory category;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "source_account_id") private TreasuryAccount sourceAccount;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "destination_account_id") private TreasuryAccount destinationAccount;
    @Column(nullable = false, precision = 18, scale = 2) private BigDecimal amount;
    @Column(name = "currency_code", nullable = false, length = 3) @Builder.Default private String currencyCode = "IRR";
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "reference_module", length = 64) private String referenceModule;
    @Column(name = "reference_number", length = 160) private String referenceNumber;
    @Column(length = 2000) private String description;
    @Column(name = "created_by") private Long createdBy;
    @Column(name = "approved_by") private Long approvedBy;
    @Column(name = "approved_at") private Instant approvedAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "cancelled_at") private Instant cancelledAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reversal_of_id") private TreasuryTransaction reversalOf;
}
