package com.sami.app.organization;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.organization.repository.BranchRepository;
import com.sami.app.organization.repository.CompanyRepository;
import com.sami.app.organization.service.BranchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class BranchTenantIsolationTest {
    @Mock BranchRepository branches;
    @Mock CompanyRepository companies;
    @Mock TenantContext tenantContext;
    @Mock JdbcTemplate jdbc;
    @InjectMocks BranchService service;

    @Test
    void listCannotUseAnotherTenantsCompanyId() {
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(companies.existsByIdAndTenantId(9L, 41L)).thenReturn(false);

        assertThatThrownBy(() -> service.list(9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
