package com.sami.app.organization.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "branches")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Branch extends BaseEntity {
    @Column(name = "tenant_id", nullable = false, updatable = false) private Long tenantId;
    @Column(name = "company_id", nullable = false) private Long companyId;
    @Column(name = "branch_type_id", nullable = false) private Long branchTypeId;
    @Column(nullable = false, length = 64) private String code;
    @Column(nullable = false, length = 255) private String name;
    @Column(nullable = false) private boolean isActive;
    @Column(name = "display_order", nullable = false) private int displayOrder;
}
