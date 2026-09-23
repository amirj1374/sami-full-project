package com.sami.app.crm.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "crm_opportunities")
@Getter @Setter @NoArgsConstructor
public class CrmOpportunity extends BaseEntity {
    @Column(name="tenant_id", nullable=false, updatable=false) private Long tenantId;
    @Column(name="customer_id") private Long customerId;
    @Column(name="lead_id") private Long leadId;
    @Column(nullable=false, length=160) private String title;
    @Column(nullable=false, length=32) private String status = "OPEN";
    @Column(name="expected_value", precision=19, scale=2) private BigDecimal expectedValue;
    @Column(name="expected_close_date") private LocalDate expectedCloseDate;
    @Column(name="assigned_user_id") private Long assignedUserId;
    @Column(length=4000) private String notes;
}
