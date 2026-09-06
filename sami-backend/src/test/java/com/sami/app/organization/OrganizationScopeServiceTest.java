package com.sami.app.organization;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import com.sami.app.authz.domain.Role;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.organization.service.FoundationAuditService;
import com.sami.app.organization.service.OrganizationScopeService;
import com.sami.app.security.SecurityUser;
import com.sami.app.user.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrganizationScopeServiceTest {
    @Mock TenantContext tenantContext; @Mock JdbcTemplate jdbc; @Mock FoundationAuditService audit;
    @InjectMocks OrganizationScopeService service;
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void alteredCompanyIdIsDeniedWithoutPositiveGrant() {
        authenticate(); when(tenantContext.requireTenantId()).thenReturn(41L);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(41L), eq(7L), eq(99L))).thenReturn(false);
        assertThatThrownBy(() -> service.requireScope(99L, null)).isInstanceOf(ApiException.class);
    }
    private void authenticate() {
        User user = User.builder().tenantId(41L).email("user@example.com").passwordHash("hash").fullName("User").role(Role.builder().build()).build();
        ReflectionTestUtils.setField(user, "id", 7L);
        SecurityUser principal = new SecurityUser(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
