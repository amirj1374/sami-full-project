package com.sami.app.siminvestment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sami.app.common.tenancy.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimInvestmentImportServiceTest {
    @Mock JdbcTemplate jdbc;
    @Mock TenantContext tenants;
    @Mock ObjectMapper json;
    @Mock SimInvestmentAnalysisService analysis;
    @InjectMocks SimInvestmentImportService service;

    @Test void importsValidRowsDeduplicatesPhonesAndKeepsZeroPriceAsEvidence() throws Exception {
        byte[] csv=("phone,price,status,seller_id\n"+
                "09126545008,210000000,کارکرده,SELLER_1\n"+
                "09126545008,210000000,کارکرده,SELLER_1\n"+
                "09120761850,0,در حدصفر,SELLER_2\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var file=new MockMultipartFile("file","0912_2026-08-21.csv","text/csv",csv);
        when(tenants.requireTenantId()).thenReturn(42L);
        when(jdbc.queryForObject(startsWith("select exists"),eq(Boolean.class),any(Object[].class))).thenReturn(false);
        when(jdbc.queryForObject(startsWith("select max"),eq(LocalDate.class),any(Object[].class))).thenReturn(null);
        when(jdbc.queryForObject(contains("returning id"),eq(Long.class),any(Object[].class))).thenReturn(9L);
        when(jdbc.queryForList(startsWith("select * from sim_investment_import_batches"),eq(42L),eq(9L)))
                .thenReturn(List.of(Map.of("id",9L,"imported_count",2,"duplicate_count",1)));
        when(analysis.recalculate()).thenReturn(Map.of("analyzed",2));
        when(json.writeValueAsString(any())).thenReturn("{}");

        Map<String,Object> result=service.importFile(file,"0912_MARKET",null,true);

        assertThat(result).containsEntry("imported_count",2).containsEntry("duplicate_count",1);
        verify(jdbc).batchUpdate(
                contains("sim_investment_listing_history"),
                argThat((List<Object[]> rows) -> rows.size()==2));
        verify(jdbc).batchUpdate(
                contains("sim_investment_import_messages"),
                argThat((List<Object[]> rows) -> rows.size()==2));
        verify(analysis).recalculate();
    }

    @Test void importsTheSuppliedSnapshotScaleWithinTheDocumentedLimit() throws Exception {
        int rowCount = 48_815;
        StringBuilder csv = new StringBuilder("phone,price,status,seller_id\n");
        for (int index = 0; index < rowCount; index++) {
            csv.append("0912").append(String.format("%07d", index))
                    .append(",179900000,کارکرده,SELLER_").append(index % 2_407).append('\n');
        }
        var file = new MockMultipartFile("file", "0912_2026-08-21.csv", "text/csv",
                csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        when(tenants.requireTenantId()).thenReturn(42L);
        when(jdbc.queryForObject(startsWith("select exists"), eq(Boolean.class), any(Object[].class))).thenReturn(false);
        when(jdbc.queryForObject(startsWith("select max"), eq(LocalDate.class), any(Object[].class))).thenReturn(null);
        when(jdbc.queryForObject(contains("returning id"), eq(Long.class), any(Object[].class))).thenReturn(10L);
        when(jdbc.queryForList(startsWith("select * from sim_investment_import_batches"), eq(42L), eq(10L)))
                .thenReturn(List.of(Map.of("id", 10L, "imported_count", rowCount, "duplicate_count", 0)));
        when(analysis.recalculate()).thenReturn(Map.of("analyzed", rowCount));
        when(json.writeValueAsString(any())).thenReturn("{}");

        Map<String, Object> result = service.importFile(file, "0912_MARKET", null, true);

        assertThat(result).containsEntry("imported_count", rowCount).containsEntry("duplicate_count", 0);
        verify(jdbc, atLeast(49)).batchUpdate(contains("sim_investment_listing_history"), org.mockito.ArgumentMatchers.<List<Object[]>>any());
        verify(analysis).recalculate();
    }
}
