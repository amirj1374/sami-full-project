package com.sami.app.sales.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "sale_return_numbers")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SaleReturnNumber extends BaseEntity {
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "next_value", nullable = false) private Long nextValue;
}
