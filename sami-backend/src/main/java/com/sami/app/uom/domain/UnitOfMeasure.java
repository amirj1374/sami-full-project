package com.sami.app.uom.domain;
import com.sami.app.common.domain.BaseEntity; import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="units_of_measure") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UnitOfMeasure extends BaseEntity { @Column(name="tenant_id",nullable=false) Long tenantId; @Column(nullable=false,length=32) String code; @Column(nullable=false,length=120) String name; @Column(nullable=false,length=24) String status; }
