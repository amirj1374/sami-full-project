package com.sami.app.sales.invoice;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name="sales_invoice_lines") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SalesInvoiceLine extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="invoice_id",nullable=false) SalesInvoice invoice;
    @Column(name="tenant_id",nullable=false) Long tenantId; @Column(name="order_line_id",nullable=false) Long orderLineId; @Column(name="delivery_line_id",nullable=false) Long deliveryLineId;
    @Column(name="product_id",nullable=false) Long productId; @Column(name="product_sku",nullable=false) String productSku; @Column(name="product_name",nullable=false) String productName;
    @Column(nullable=false) BigDecimal quantity; @Column(name="unit_price",nullable=false) BigDecimal unitPrice; @Column(nullable=false) BigDecimal discount; @Column(name="line_total",nullable=false) BigDecimal lineTotal;
}
