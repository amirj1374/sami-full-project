package com.sami.app.supplier.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A contact person at a supplier; at most one primary per supplier. */
@Entity
@Table(name = "sup_contacts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupContact extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(length = 100)
    private String position;

    @Column(length = 100)
    private String department;

    @Column(length = 32)
    private String phone;

    @Column(length = 32)
    private String mobile;

    @Column(length = 255)
    private String email;

    @Column(name = "preferred_method", length = 32)
    private String preferredMethod;

    @Column(length = 500)
    private String notes;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;
}
