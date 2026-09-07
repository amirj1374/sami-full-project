package com.sami.app.sales.order;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name="sales_order_lines") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SalesOrderLine extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_id", nullable=false) SalesOrder order;
    @Column(name="tenant_id", nullable=false) Long tenantId;
    @Column(name="source_line_id") Long sourceLineId;
    @Column(name="product_id", nullable=false) Long productId;
    @Column(name="product_sku", nullable=false) String productSku;
    @Column(name="product_name", nullable=false) String productName;
    @Column(nullable=false) BigDecimal quantity;
    @Column(name="unit_price", nullable=false) BigDecimal unitPrice;
    @Column(nullable=false) BigDecimal discount;
    @Column(name="line_total", nullable=false) BigDecimal lineTotal;
    @Column(name="reserved_quantity", nullable=false) @Builder.Default BigDecimal reservedQuantity=BigDecimal.ZERO;
    @Column(name="backordered_quantity", nullable=false) @Builder.Default BigDecimal backorderedQuantity=BigDecimal.ZERO;
    @Column(name="fulfilled_quantity", nullable=false) @Builder.Default BigDecimal fulfilledQuantity=BigDecimal.ZERO;
}
