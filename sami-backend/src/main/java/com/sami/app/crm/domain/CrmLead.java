package com.sami.app.crm.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "crm_leads")
@Getter @Setter @NoArgsConstructor
public class CrmLead extends BaseEntity {
    @Column(name="tenant_id", nullable=false, updatable=false) private Long tenantId;
    @Column(name="customer_id") private Long customerId;
    @Column(nullable=false, length=160) private String title;
    @Column(nullable=false, length=32) private String status = "OPEN";
    @Column(length=64) private String source;
    @Column(name="assigned_user_id") private Long assignedUserId;
    @Column(length=4000) private String notes;
}
