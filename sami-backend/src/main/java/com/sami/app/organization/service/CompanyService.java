package com.sami.app.organization.service;

import static com.sami.app.organization.dto.CompanyDtos.CompanyRequest;
import static com.sami.app.organization.dto.CompanyDtos.CompanyResponse;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.organization.domain.Company;
import com.sami.app.organization.repository.CompanyRepository;
import com.sami.app.security.CurrentActor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companies;
    private final TenantContext tenantContext;
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<CompanyResponse> list() {
        return companies.findByTenantIdOrderByDisplayOrderAscNameAsc(tenantContext.requireTenantId()).stream()
                .map(CompanyResponse::from).toList();
    }

    @Transactional
    public CompanyResponse create(CompanyRequest request) {
        Long tenantId = tenantContext.requireTenantId();
        String code = request.code().trim();
        if (companies.existsByTenantIdAndCodeIgnoreCase(tenantId, code)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "Company code already exists");
        }
        Company company = Company.builder().tenantId(tenantId).code(code).name(request.name().trim())
                .currencyCode(valueOr(request.currencyCode(), "IRR"))
                .timezone(valueOr(request.timezone(), "Asia/Tehran"))
                .locale(valueOr(request.locale(), "fa"))
                .fiscalYearStartMonth((short) valueOr(request.fiscalYearStartMonth(), 1).intValue())
                .isActive(request.active() == null || request.active()).displayOrder(valueOr(request.displayOrder(), 0))
                .isDefault(false).build();
        apply(company, request);
        try {
            company = companies.save(company);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "Company code already exists");
        }
        audit(tenantId, company, "CREATED");
        return CompanyResponse.from(company);
    }

    @Transactional
    public CompanyResponse update(Long id, CompanyRequest request) {
        Long tenantId = tenantContext.requireTenantId();
        Company company = company(id, tenantId);
        if (company.isDefault() && Boolean.FALSE.equals(request.active())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "Default company cannot be inactive");
        }
        if (request.expectedVersion() == null || !request.expectedVersion().equals(company.getVersion())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "Company changed; refresh and retry");
        }
        String code = request.code().trim();
        if (companies.existsByTenantIdAndCodeIgnoreCaseAndIdNot(tenantId, code, id)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "Company code already exists");
        }
        company.setCode(code);
        company.setName(request.name().trim());
        apply(company, request);
        try {
            company = companies.save(company);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "Company code already exists");
        }
        audit(tenantId, company, "UPDATED");
        return CompanyResponse.from(company);
    }

    private Company company(Long id, Long tenantId) {
        return companies.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Company", id));
    }

    private void apply(Company company, CompanyRequest request) {
        company.setLegalName(blank(request.legalName()));
        company.setTaxNumber(blank(request.taxNumber()));
        company.setRegistrationNumber(blank(request.registrationNumber()));
        company.setCurrencyCode(valueOr(request.currencyCode(), company.getCurrencyCode()));
        company.setTimezone(valueOr(request.timezone(), company.getTimezone()));
        company.setLocale(valueOr(request.locale(), company.getLocale()));
        company.setFiscalYearStartMonth((short) valueOr(request.fiscalYearStartMonth(), (int) company.getFiscalYearStartMonth()).intValue());
        company.setEmail(blank(request.email()));
        company.setPhone(blank(request.phone()));
        company.setWebsite(blank(request.website()));
        company.setAddressLine1(blank(request.addressLine1()));
        company.setCity(blank(request.city()));
        company.setState(blank(request.state()));
        company.setPostalCode(blank(request.postalCode()));
        company.setCountryCode(blank(request.countryCode()));
        if (request.active() != null) company.setActive(request.active());
        if (request.displayOrder() != null) company.setDisplayOrder(request.displayOrder());
    }

    private void audit(Long tenantId, Company company, String action) {
        jdbc.update("insert into organization_audit_logs(tenant_id, company_id, action, actor_id, actor_email, detail) values(?,?,?,?,?,jsonb_build_object('code', ?))",
                tenantId, company.getId(), action, CurrentActor.id(), CurrentActor.email(), company.getCode());
    }

    private static String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static <T> T valueOr(T value, T fallback) { return value == null || (value instanceof String text && text.isBlank()) ? fallback : value; }
}
