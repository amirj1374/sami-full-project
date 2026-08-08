package com.sami.app.hamta;

import com.sami.app.common.tenancy.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HamtaReportFilterTest {

    @Mock JdbcTemplate jdbc;
    @Mock TenantContext tenantContext;

    @Test
    void reportOmitsNullablePredicatesInsteadOfBindingUntypedNulls() {
        when(tenantContext.requireTenantId()).thenReturn(71L);
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());

        new HamtaService(jdbc, tenantContext).report(null, null);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> parameters = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).queryForList(sql.capture(), parameters.capture());
        assertThat(sql.getValue()).contains("where h.tenant_id=?")
                .doesNotContain("is null")
                .doesNotContain("h.delivered=?")
                .doesNotContain("p.id=?");
        assertThat(parameters.getValue()).containsExactly(71L);
    }

    @Test
    void reportAddsOnlyPresentFiltersWithTheirTypedValues() {
        when(tenantContext.requireTenantId()).thenReturn(71L);
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());

        new HamtaService(jdbc, tenantContext).report(Boolean.FALSE, 19L);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> parameters = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).queryForList(sql.capture(), parameters.capture());
        assertThat(sql.getValue()).contains("h.delivered=?").contains("p.id=?");
        assertThat(parameters.getValue()).containsExactly(71L, Boolean.FALSE, 19L);
    }
}
