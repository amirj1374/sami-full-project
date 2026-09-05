package com.sami.app.organization.dto;

import com.sami.app.organization.domain.Company;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class CompanyDtos {
    private CompanyDtos() { }

    public record CompanyRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 255) String name,
            @Size(max = 255) String legalName,
            @Size(max = 64) String taxNumber,
            @Size(max = 64) String registrationNumber,
            @Size(max = 8) String currencyCode,
            @Size(max = 64) String timezone,
            @Size(max = 16) String locale,
            @Min(1) @Max(12) Integer fiscalYearStartMonth,
            @Size(max = 255) String email,
            @Size(max = 64) String phone,
            @Size(max = 255) String website,
            @Size(max = 255) String addressLine1,
            @Size(max = 128) String city,
            @Size(max = 128) String state,
            @Size(max = 32) String postalCode,
            @Size(max = 8) String countryCode,
            Boolean active,
            @Min(0) Integer displayOrder,
            Long expectedVersion
    ) { }

    public record CompanyResponse(
            Long id, String code, String name, String legalName, String taxNumber,
            String registrationNumber, String currencyCode, String timezone, String locale,
            short fiscalYearStartMonth, String email, String phone, String website,
            String addressLine1, String city, String state, String postalCode, String countryCode,
            boolean isDefault, boolean active, int displayOrder, Long version,
            Instant createdAt, Instant updatedAt
    ) {
        public static CompanyResponse from(Company company) {
            return new CompanyResponse(company.getId(), company.getCode(), company.getName(),
                    company.getLegalName(), company.getTaxNumber(), company.getRegistrationNumber(),
                    company.getCurrencyCode(), company.getTimezone(), company.getLocale(),
                    company.getFiscalYearStartMonth(), company.getEmail(), company.getPhone(),
                    company.getWebsite(), company.getAddressLine1(), company.getCity(), company.getState(),
                    company.getPostalCode(), company.getCountryCode(), company.isDefault(), company.isActive(),
                    company.getDisplayOrder(), company.getVersion(), company.getCreatedAt(), company.getUpdatedAt());
        }
    }
}
