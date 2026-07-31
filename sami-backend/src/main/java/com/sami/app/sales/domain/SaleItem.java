package com.sami.app.sales.domain;
import com.sami.app.common.domain.BaseEntity; import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal;
@Entity @Table(name="sale_items") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SaleItem extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="sale_id") private Sale sale;
 @Column(name="tenant_id",nullable=false) private Long tenantId; @Column(name="product_id",nullable=false) private Long productId;
 @Column(name="product_sku",nullable=false) private String productSku; @Column(name="product_name",nullable=false) private String productName;
 @Column(name="serial_number") private String serialNumber; @Column private String imei;
 @Column(nullable=false,precision=14,scale=3) private BigDecimal quantity;
 @Column(name="returned_quantity",nullable=false,precision=14,scale=3) @Builder.Default private BigDecimal returnedQuantity=BigDecimal.ZERO;
 @Column(name="unit_price",nullable=false,precision=18,scale=2) private BigDecimal unitPrice;
 @Column(name="cost_price",nullable=false,precision=18,scale=2) private BigDecimal costPrice;
 @Column(nullable=false,precision=18,scale=2) @Builder.Default private BigDecimal discount=BigDecimal.ZERO;
 @Column(nullable=false,precision=18,scale=2) @Builder.Default private BigDecimal tax=BigDecimal.ZERO;
 @Column(name="line_total",nullable=false,precision=18,scale=2) private BigDecimal lineTotal;
 @Column(nullable=false,precision=18,scale=2) private BigDecimal profit;
}
