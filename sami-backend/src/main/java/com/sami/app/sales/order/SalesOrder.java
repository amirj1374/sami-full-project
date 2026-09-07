package com.sami.app.sales.order;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="sales_orders") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SalesOrder extends BaseEntity {
    @Column(name="tenant_id", nullable=false) Long tenantId;
    @Column(name="company_id", nullable=false) Long companyId;
    @Column(name="branch_id", nullable=false) Long branchId;
    @Column(name="customer_id", nullable=false) Long customerId;
    @Column(name="contact_id", nullable=false) Long contactId;
    @Column(name="order_number", nullable=false) String orderNumber;
    @Column(name="source_document_id") Long sourceDocumentId;
    @Enumerated(EnumType.STRING) @Column(nullable=false) SalesOrderStatus status;
    @Column(nullable=false) @Builder.Default String currency="IRR";
    @Column(nullable=false) @Builder.Default BigDecimal subtotal=BigDecimal.ZERO;
    @Column(name="discount_total", nullable=false) @Builder.Default BigDecimal discountTotal=BigDecimal.ZERO;
    @Column(name="final_amount", nullable=false) @Builder.Default BigDecimal finalAmount=BigDecimal.ZERO;
    @Column(name="confirmed_at") Instant confirmedAt;
    @Column(name="cancelled_at") Instant cancelledAt;
    @Column(name="created_by") Long createdBy;
    @Column(name="created_by_email") String createdByEmail;
    @Column(length=2000) String notes;
    @OneToMany(mappedBy="order", cascade=CascadeType.ALL, orphanRemoval=true) @Builder.Default List<SalesOrderLine> lines=new ArrayList<>();
}
