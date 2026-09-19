package com.sami.app.product.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_variants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductVariant extends BaseEntity {
    @Column(name="tenant_id", nullable=false) private Long tenantId;
    @Column(name="product_id", nullable=false) private Long productId;
    @Column(name="variant_code", nullable=false, length=64) private String variantCode;
    @Column(nullable=false, length=255) private String name;
    @Column(length=64) private String sku;
    @Column(nullable=false, length=24) private String status;
    @Column(name="base_uom_id") private Long baseUomId;
}
