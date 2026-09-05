package com.sami.app.organization;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.organization.repository.CompanyRepository;
import com.sami.app.organization.service.CompanyService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class CompanyTenantIsolationTest {
    @Mock CompanyRepository companies;
    @Mock TenantContext tenantContext;
    @Mock JdbcTemplate jdbc;
    @InjectMocks CompanyService service;

    @Test
    void updateCannotSeeAnotherTenantsCompany() {
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(companies.findByIdAndTenantId(9L, 41L)).thenReturn(Optional.empty());

        var request = new com.sami.app.organization.dto.CompanyDtos.CompanyRequest(
                "OTHER", "Other", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, true, 0, 1L);

        assertThatThrownBy(() -> service.update(9L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(companies).findByIdAndTenantId(9L, 41L);
    }
}
