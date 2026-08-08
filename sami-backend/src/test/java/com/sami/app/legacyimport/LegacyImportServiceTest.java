package com.sami.app.legacyimport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.storage.FileStorage;
import com.sami.app.common.tenancy.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LegacyImportServiceTest {
    @Mock JdbcTemplate jdbc;
    @Mock TenantContext tenants;
    @Mock FileStorage storage;
    @Mock ObjectMapper json;
    @Mock AsanLegacyImportAdapter adapter;
    @Mock LegacyImportProperties properties;
    @InjectMocks LegacyImportService service;

    @Test void listUsesOnlyTheTrustedTenantScope() {
        when(tenants.requireTenantId()).thenReturn(42L);
        when(jdbc.queryForList(anyString(), eq(42L))).thenReturn(List.of());

        service.list();

        verify(jdbc).queryForList(contains("WHERE tenant_id=?"), eq(42L));
        verify(tenants).requireTenantId();
    }

    @Test void duplicateArchiveHashDeletesTheNewStoredObject() throws Exception {
        MultipartFile file=mock(MultipartFile.class);
        byte[] rar={0x52,0x61,0x72,0x21,0x1a,0x07,0x00};
        when(file.isEmpty()).thenReturn(false); when(file.getSize()).thenReturn((long)rar.length);
        when(file.getOriginalFilename()).thenReturn("safe.rar"); when(file.getBytes()).thenReturn(rar);
        when(properties.maxUploadBytes()).thenReturn(1024L); when(tenants.requireTenantId()).thenReturn(42L);
        when(storage.store(eq("legacy-imports/42"),same(rar),eq("application/vnd.rar"))).thenReturn("opaque-key");
        when(adapter.sourceSystem()).thenReturn("ASAN");
        when(jdbc.queryForObject(contains("INSERT INTO legacy_import_batches"),eq(Long.class),any(Object[].class)))
                .thenThrow(new DuplicateKeyException("duplicate"));

        assertThatThrownBy(() -> service.upload(file)).isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST))
                .hasMessageContaining("already been uploaded");
        verify(storage).delete("opaque-key");
    }

    @Test void comparisonIsTenantScopedAndUsesOnlyExactCustomerCode() throws Exception {
        when(tenants.requireTenantId()).thenReturn(73L);
        when(jdbc.queryForMap(anyString(),eq(73L),eq(8L))).thenReturn(Map.of("status","COMPLETED"));
        when(jdbc.queryForObject(anyString(),eq(Long.class),any(Object[].class))).thenReturn(9L,5L,2L,1L);
        when(jdbc.queryForList(contains("LEFT JOIN customers"),eq(73L),eq(8L))).thenReturn(List.of());
        when(json.writeValueAsString(any())).thenReturn("{}");

        Map<String,Object> result=service.compare(8L);

        assertThat(result).containsEntry("matchedByCustomerCode",1L).containsEntry("unmapped",4L);
        assertThat(result.get("matchingPolicy")).asString().contains("Exact tenant-scoped customer_code only");
        verify(jdbc).queryForList(contains("c.tenant_id=r.tenant_id"),eq(73L),eq(8L));
        verify(jdbc,never()).update(contains("UPDATE customers"),any(Object[].class));
    }
}
