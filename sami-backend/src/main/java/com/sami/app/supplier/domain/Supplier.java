package com.sami.app.supplier.domain;

import com.sami.app.common.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The central supplier record — the single source of supplier truth for the
 * whole ERP. Purchasing (and any future module) references this table; no
 * purchasing/accounting/treasury logic lives in this module.
 */
@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier extends BaseEntity {

    @Column(name = "supplier_code", nullable = false, unique = true, length = 32)
    private String supplierCode;

    @Column(name = "company_name", nullable = false, length = 160)
    private String companyName;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "legal_name", length = 160)
    private String legalName;

    @Column(name = "national_id", length = 32)
    private String nationalId;

    @Column(name = "economic_code", length = 32)
    private String economicCode;

    @Column(name = "tax_number", length = 64)
    private String taxNumber;

    @Column(name = "registration_number", length = 64)
    private String registrationNumber;

    @Column(name = "owner_name", length = 120)
    private String ownerName;

    @Column(length = 255)
    private String website;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String province;

    @Column(length = 100)
    private String city;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false)
    private SupType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private SupStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_term_id")
    private SupPaymentTerm paymentTerm;

    @Column(name = "credit_limit", precision = 14, scale = 2)
    private BigDecimal creditLimit;

    /** Weight-averaged rating across active criteria, cached for filtering. */
    @Column(name = "rating_avg", precision = 4, scale = 2)
    private BigDecimal ratingAvg;

    /** Provenance for records migrated from the CRM registry. */
    @Column(name = "source_customer_id")
    private Long sourceCustomerId;

    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("isDefault DESC, id ASC")
    @Builder.Default
    private List<SupChannel> channels = new ArrayList<>();

    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("isDefault DESC, id ASC")
    @Builder.Default
    private List<SupAddress> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("isPrimary DESC, id ASC")
    @Builder.Default
    private List<SupContact> contacts = new ArrayList<>();

    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("isDefault DESC, id ASC")
    @Builder.Default
    private List<SupBankAccount> bankAccounts = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "sup_tag_links",
            joinColumns = @JoinColumn(name = "supplier_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @Builder.Default
    private Set<SupTag> tags = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "sup_category_links",
            joinColumns = @JoinColumn(name = "supplier_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    @Builder.Default
    private Set<SupCategory> categories = new HashSet<>();

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "archived_by")
    private Long archivedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;
}
