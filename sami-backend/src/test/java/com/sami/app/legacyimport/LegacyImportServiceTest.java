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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

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
    @Mock LegacyImportAdapter adapter;
    @Mock List<LegacyImportAdapter> adapters;
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
        when(adapters.stream()).thenReturn(Stream.of(adapter)); when(adapter.supports("safe.rar",rar)).thenReturn(true);
        when(adapter.mediaType()).thenReturn("application/vnd.rar"); when(adapter.parserVersion()).thenReturn("asan-1.0");
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
        when(jdbc.queryForObject(anyString(),eq(Long.class),any(Object[].class)))
                .thenReturn(9L,10L,3L,4L,1L,1L,0L,2L);
        when(jdbc.queryForList(contains("LEFT JOIN customers"),eq(73L),eq(8L))).thenReturn(List.of());
        when(json.writeValueAsString(any())).thenReturn("{}");

        Map<String,Object> result=service.compare(8L);

        assertThat(result).containsEntry("matchedByCustomerCode",1L)
                .containsEntry("matchedBySupplierCode",1L)
                .containsEntry("matchedByProductSku",2L)
                .containsEntry("unmapped",6L);
        assertThat(result.get("matchingPolicy")).asString().contains("Exact tenant-scoped customer/supplier codes");
        verify(jdbc).queryForList(contains("c.tenant_id=r.tenant_id"),eq(73L),eq(8L));
        verify(jdbc,never()).update(contains("UPDATE customers"),any(Object[].class));
    }

    @Test void reconciliationExposesStoredSummaryForTheCustomerUi() throws Exception {
        when(tenants.requireTenantId()).thenReturn(42L);
        when(jdbc.queryForMap(anyString(), eq(42L), eq(7L))).thenReturn(Map.of(
                "id", 7L,
                "status", "READY_FOR_ACCEPTANCE",
                "acceptance_status", "BLOCKED",
                "metadata", "{\"journalRows\":33796,\"trialBalanceRows\":32,\"chequeRows\":599,\"stagingOnly\":true}"));
        when(json.readValue(anyString(), eq(Map.class))).thenReturn(Map.of(
                "journalRows", 33796,
                "trialBalanceRows", 32,
                "chequeRows", 599,
                "stagingOnly", true));
        when(jdbc.queryForList(anyString(), eq(42L), eq(7L))).thenReturn(List.of());

        Map<String,Object> result = service.reconciliation(7L);

        assertThat(result).containsEntry("journalRows", 33796)
                .containsEntry("trialBalanceRows", 32)
                .containsEntry("chequeRows", 599)
                .containsEntry("stagingOnly", true);
    }

    @Test void executeStreamsAccountingRowsInConfiguredChunksWithoutMaterializingRecords() throws Exception {
        long tenant = 42L;
        long batchId = 8L;
        byte[] source = {0x50, 0x4b, 0x03, 0x04};
        String filename = "daily.xlsx";
        String datasetKey = filename + "#دفتر روزنامه";
        var analysis = new LegacyImportAdapter.Analysis(
                List.of(new LegacyImportAdapter.SourceFile(filename, filename, source.length, "hash", "XLSX", "SUPPORTED", Map.of())),
                List.of(new LegacyImportAdapter.Dataset(filename, datasetKey, "دفتر روزنامه", "ACCOUNTING_JOURNAL",
                        "SUPPORTED", "UTF-8", 3, List.of(), Map.of(), List.of())),
                List.of());

        when(tenants.requireTenantId()).thenReturn(tenant);
        when(jdbc.queryForMap(anyString(), eq(tenant), eq(batchId)))
                .thenReturn(Map.of("status", "READY", "original_filename", filename));
        when(jdbc.queryForObject(contains("SELECT storage_key"), eq(String.class), eq(tenant), eq(batchId))).thenReturn("opaque-key");
        when(storage.load("opaque-key")).thenReturn(Optional.of(new FileStorage.StoredFile(
                source, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
        when(adapters.stream()).thenReturn(Stream.of(adapter));
        when(adapter.supports(filename, source)).thenReturn(true);
        when(adapter.supportsStreamingRecords()).thenReturn(true);
        when(adapter.analyze(filename, source, false)).thenReturn(analysis);
        when(jdbc.queryForObject(contains("INSERT INTO legacy_import_files"), eq(Long.class), any(Object[].class))).thenReturn(101L);
        when(jdbc.queryForObject(contains("INSERT INTO legacy_datasets"), eq(Long.class), any(Object[].class))).thenReturn(201L);
        when(properties.importChunkSize()).thenReturn(2);
        when(json.writeValueAsString(any())).thenReturn("{}");
        List<Integer> chunkSizes = new ArrayList<>();
        when(jdbc.batchUpdate(anyString(), anyList())).thenAnswer(invocation -> {
            int size = ((List<?>) invocation.getArgument(1)).size();
            chunkSizes.add(size);
            return new int[size];
        });
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            BiConsumer<String, LegacyImportAdapter.Record> consumer = invocation.getArgument(2);
            for (int index = 1; index <= 3; index++) {
                consumer.accept(datasetKey, new LegacyImportAdapter.Record(
                        "دفتر روزنامه:" + index, String.valueOf(index), String.valueOf(index),
                        Map.of("sourceRowNumber", index), Map.of("debit", index)));
            }
            return null;
        }).when(adapter).streamRecords(eq(filename), same(source), any());

        Map<String,Object> result = service.execute(batchId);

        assertThat(result).containsEntry("status", "READY");
        assertThat(chunkSizes).containsExactly(2, 1);
        verify(adapter).analyze(filename, source, false);
        verify(adapter, never()).analyze(filename, source, true);
        verify(adapter).streamRecords(eq(filename), same(source), any());
        verify(jdbc).update(contains("UPDATE legacy_datasets SET imported_record_count"),
                eq(3L), eq(tenant), eq(batchId), eq(201L));
        verify(jdbc, never()).update(contains("UPDATE customers"), any(Object[].class));
        verify(jdbc, never()).update(contains("UPDATE products"), any(Object[].class));
    }
}
