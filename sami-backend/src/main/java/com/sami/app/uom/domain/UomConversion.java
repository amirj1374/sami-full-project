package com.sami.app.uom.domain;
import com.sami.app.common.domain.BaseEntity; import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal;
@Entity @Table(name="uom_conversions") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UomConversion extends BaseEntity { @Column(name="tenant_id",nullable=false) Long tenantId; @Column(name="from_uom_id",nullable=false) Long fromUomId; @Column(name="to_uom_id",nullable=false) Long toUomId; @Column(nullable=false,precision=24,scale=12) BigDecimal factor; @Column(nullable=false,length=24) String status; }
