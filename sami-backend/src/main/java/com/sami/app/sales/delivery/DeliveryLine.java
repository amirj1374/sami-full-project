package com.sami.app.sales.delivery;

import com.sami.app.common.domain.BaseEntity;
import com.sami.app.sales.order.SalesOrderLine;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name="sales_delivery_lines") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DeliveryLine extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="delivery_id",nullable=false) Delivery delivery;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_line_id",nullable=false) SalesOrderLine orderLine;
    @Column(name="order_line_id",insertable=false,updatable=false) Long orderLineId;
    @Column(name="tenant_id",nullable=false) Long tenantId;
    @Column(name="product_id",nullable=false) Long productId; @Column(name="product_sku",nullable=false) String productSku; @Column(name="product_name",nullable=false) String productName;
    @Column(name="ordered_quantity",nullable=false) BigDecimal orderedQuantity; @Column(name="reserved_quantity",nullable=false) BigDecimal reservedQuantity;
    @Column(name="previously_delivered_quantity",nullable=false) BigDecimal previouslyDeliveredQuantity; @Column(name="delivery_quantity",nullable=false) BigDecimal deliveryQuantity;
    @Column(name="backordered_quantity",nullable=false) BigDecimal backorderedQuantity;
}
