package com.sami.app.sales.domain;
import com.sami.app.common.domain.BaseEntity; import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.Instant;
@Entity @Table(name="sale_payments") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SalePayment extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="sale_id") private Sale sale;
 @Column(name="tenant_id",nullable=false) private Long tenantId; @Column(nullable=false) private String method;
 @Column(nullable=false) @Builder.Default private String status="CAPTURED"; @Column(nullable=false,precision=18,scale=2) private BigDecimal amount;
 @Column(name="reference_no") private String referenceNo; @Column(name="provider_reference") private String providerReference;
 @Column(name="reversed_amount",nullable=false,precision=18,scale=2) @Builder.Default private BigDecimal reversedAmount=BigDecimal.ZERO;
 @Column(name="paid_at",nullable=false) @Builder.Default private Instant paidAt=Instant.now(); @Column(name="created_by") private Long createdBy;
}
