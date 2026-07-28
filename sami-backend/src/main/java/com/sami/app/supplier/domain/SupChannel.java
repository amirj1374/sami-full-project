package com.sami.app.supplier.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A supplier-level phone or email; at most one default per kind. */
@Entity
@Table(name = "sup_channels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupChannel extends BaseEntity {

    public enum Kind { PHONE, EMAIL }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Kind kind;

    @Column(nullable = false, length = 255)
    private String value;

    @Column(length = 60)
    private String label;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;
}
