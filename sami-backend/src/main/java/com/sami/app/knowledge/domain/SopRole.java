package com.sami.app.knowledge.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A role responsible for a procedure. Identified by role id where the ERP models
 * that role, or by a free-text label where it does not yet — so an SOP can name
 * "Workshop Technician" before an HR module exists.
 */
@Entity
@Table(name = "kb_sop_roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SopRole extends BaseEntity {

    @Column(name = "sop_id", nullable = false) private Long sopId;
    @Column(name = "role_id") private Long roleId;
    @Column(name = "role_label", length = 160) private String roleLabel;
    @Column(length = 1000) private String responsibility;
    @Column(name = "is_accountable", nullable = false) private boolean isAccountable;
    @Column(name = "display_order", nullable = false) private int displayOrder;
}
