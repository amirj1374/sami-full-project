package com.sami.app.sales.domain;
import com.sami.app.common.domain.BaseEntity; import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal;
@Entity @Table(name="sale_return_items") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SaleReturnItem extends BaseEntity { @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="return_id") private SaleReturn saleReturn; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sale_item_id") private SaleItem saleItem; @Column private BigDecimal quantity; @Column private BigDecimal amount; }
