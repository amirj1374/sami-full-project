package com.sami.app.sales.domain;
import com.sami.app.common.domain.BaseEntity; import jakarta.persistence.*; import lombok.*; import java.math.BigDecimal;
@Entity @Table(name="sale_services") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SaleServiceLine extends BaseEntity { @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sale_id") private Sale sale; @Column(name="tenant_id") private Long tenantId; @Column(name="service_type") private String serviceType; @Column private String description; @Column private BigDecimal price; @Column private BigDecimal cost; @Column(name="employee_id") private Long employeeId; }
