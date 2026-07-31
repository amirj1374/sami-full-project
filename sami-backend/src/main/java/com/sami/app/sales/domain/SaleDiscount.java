package com.sami.app.sales.domain;
import com.sami.app.common.domain.BaseEntity; import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal; import java.time.Instant;
@Entity @Table(name="sale_discounts") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SaleDiscount extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="sale_id") private Sale sale; @Column(name="tenant_id",nullable=false) private Long tenantId;
 @Column(name="discount_type",nullable=false) private String discountType; @Column(nullable=false,precision=18,scale=2) private BigDecimal amount;
 @Column(nullable=false) private String reason; @Column(nullable=false) private String status; @Column(name="requested_by") private Long requestedBy;
 @Column(name="approved_by") private Long approvedBy; @Column(name="requested_at",nullable=false) @Builder.Default private Instant requestedAt=Instant.now(); @Column(name="decided_at") private Instant decidedAt;
}
