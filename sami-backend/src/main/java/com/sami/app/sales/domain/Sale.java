package com.sami.app.sales.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="sales") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Sale extends BaseEntity {
 @Column(name="tenant_id",nullable=false) private Long tenantId;
 @Column(name="company_id",nullable=false) private Long companyId;
 @Column(name="branch_id",nullable=false) private Long branchId;
 @Column(name="invoice_number",nullable=false,length=40) private String invoiceNumber;
 @Column(name="idempotency_key",length=100) private String idempotencyKey;
 @Column(name="customer_id",nullable=false) private Long customerId;
 @Column(name="seller_id",nullable=false) private Long sellerId;
 @Column(name="sale_type",nullable=false,length=32) private String saleType;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private SaleStatus status;
 @Column(nullable=false,length=3) @Builder.Default private String currency="IRR";
 @Column(nullable=false,precision=18,scale=2) @Builder.Default private BigDecimal subtotal=BigDecimal.ZERO;
 @Column(name="discount_total",nullable=false,precision=18,scale=2) @Builder.Default private BigDecimal discountTotal=BigDecimal.ZERO;
 @Column(name="tax_total",nullable=false,precision=18,scale=2) @Builder.Default private BigDecimal taxTotal=BigDecimal.ZERO;
 @Column(name="cost_total",nullable=false,precision=18,scale=2) @Builder.Default private BigDecimal costTotal=BigDecimal.ZERO;
 @Column(name="final_amount",nullable=false,precision=18,scale=2) @Builder.Default private BigDecimal finalAmount=BigDecimal.ZERO;
 @Column(nullable=false,precision=18,scale=2) @Builder.Default private BigDecimal profit=BigDecimal.ZERO;
 @Column(name="commission_amount",nullable=false,precision=18,scale=2) @Builder.Default private BigDecimal commissionAmount=BigDecimal.ZERO;
 @Column(length=2000) private String notes;
 @Column(name="confirmed_at") private Instant confirmedAt;
 @Column(name="completed_at") private Instant completedAt;
 @Column(name="cancelled_at") private Instant cancelledAt;
 @Column(name="cancellation_reason",length=500) private String cancellationReason;
 @Column(name="created_by") private Long createdBy;
 @Column(name="created_by_email") private String createdByEmail;
 @OneToMany(mappedBy="sale",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("id ASC") @Builder.Default private List<SaleItem> items=new ArrayList<>();
 @OneToMany(mappedBy="sale",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("id ASC") @Builder.Default private List<SalePayment> payments=new ArrayList<>();
 @OneToMany(mappedBy="sale",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("id ASC") @Builder.Default private List<SaleServiceLine> services=new ArrayList<>();
}
