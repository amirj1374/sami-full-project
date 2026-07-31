package com.sami.app.sales.domain;
import com.sami.app.common.domain.BaseEntity; import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="sale_numbers") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SaleNumber extends BaseEntity { @Column(name="tenant_id",nullable=false) private Long tenantId; @Column(name="sequence_year",nullable=false) private int sequenceYear; @Column(name="next_value",nullable=false) private long nextValue; }
