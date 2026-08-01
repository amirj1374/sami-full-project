package com.sami.app.sales.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "lost_sales")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LostSale extends BaseEntity {
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "company_id", nullable = false) private Long companyId;
    @Column(name = "branch_id", nullable = false) private Long branchId;
    @Column(name = "customer_id") private Long customerId;
    @Column(name = "product_id") private Long productId;
    @Column(name = "seller_id", nullable = false) private Long sellerId;
    @Column(name = "reason_code", nullable = false, length = 40) private String reasonCode;
    @Column(length = 1000) private String notes;
    @Column(name = "expected_amount", nullable = false, precision = 18, scale = 2) private BigDecimal expectedAmount;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
}
