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

/** Tenant-owned legal entity. Branch management remains a separate workflow. */
@Entity
@Table(name = "companies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Company extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "legal_name", length = 255)
    private String legalName;

    @Column(name = "tax_number", length = 64)
    private String taxNumber;

    @Column(name = "registration_number", length = 64)
    private String registrationNumber;

    @Column(name = "currency_code", nullable = false, length = 8)
    private String currencyCode;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(nullable = false, length = 16)
    private String locale;

    @Column(name = "fiscal_year_start_month", nullable = false)
    private short fiscalYearStartMonth;

    @Column(length = 255)
    private String email;

    @Column(length = 64)
    private String phone;

    @Column(length = 255)
    private String website;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(length = 128)
    private String city;

    @Column(name = "state", length = 128)
    private String state;

    @Column(name = "postal_code", length = 32)
    private String postalCode;

    @Column(name = "country_code", length = 8)
    private String countryCode;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;
}
