package com.sami.app.sales.invoice;

import com.sami.app.common.domain.BaseEntity;
import com.sami.app.sales.order.SalesOrder;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="sales_invoices") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SalesInvoice extends BaseEntity {
    @Column(name="tenant_id",nullable=false) Long tenantId;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_id",nullable=false) SalesOrder order;
    @Column(name="order_id",insertable=false,updatable=false) Long orderId;
    @Column(name="delivery_id") Long deliveryId;
    @Column(name="company_id",nullable=false) Long companyId; @Column(name="branch_id",nullable=false) Long branchId;
    @Column(name="customer_id",nullable=false) Long customerId; @Column(name="contact_id",nullable=false) Long contactId;
    @Column(name="invoice_number",nullable=false) String invoiceNumber;
    @Enumerated(EnumType.STRING) @Column(nullable=false) InvoiceStatus status;
    @Column(nullable=false) @Builder.Default String currency="IRR";
    @Column(nullable=false) @Builder.Default BigDecimal subtotal=BigDecimal.ZERO;
    @Column(name="discount_total",nullable=false) @Builder.Default BigDecimal discountTotal=BigDecimal.ZERO;
    @Column(name="final_amount",nullable=false) @Builder.Default BigDecimal finalAmount=BigDecimal.ZERO;
    @Column(name="receivable_posting_key") String receivablePostingKey; @Column(name="receivable_posted_at") Instant receivablePostedAt;
    @Column(name="issued_at") Instant issuedAt; @Column(name="cancelled_at") Instant cancelledAt;
    @Column(name="created_by") Long createdBy; @Column(name="created_by_email") String createdByEmail; @Column(length=2000) String notes;
    @OneToMany(mappedBy="invoice",cascade=CascadeType.ALL,orphanRemoval=true) @Builder.Default List<SalesInvoiceLine> lines=new ArrayList<>();
}
