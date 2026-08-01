package com.sami.app.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.inventory.dto.InventoryDtos.AdjustmentRequest;
import com.sami.app.inventory.publicapi.InventoryStockOperations;
import com.sami.app.inventory.service.InventoryLedgerService;
import com.sami.app.inventory.service.InventoryWorkflowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryWorkflowServiceTest {

    @Mock TenantContext tenantContext;
    @Mock JdbcTemplate jdbc;
    @Mock ObjectMapper objectMapper;
    @Mock InventoryLedgerService ledger;
    @Mock InventoryStockOperations stockOperations;
    @InjectMocks InventoryWorkflowService service;

    @Test
    void adjustmentPostsInAndOutThroughCanonicalLedger() {
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(ledger.requireLocation(41L, 7L, null)).thenReturn(8L);
        AdjustmentRequest request = new AdjustmentRequest(7L, null, "Cycle correction", "key-1",
                List.of(
                        new AdjustmentRequest.AdjustmentLine(10L, new BigDecimal("3"),
                                new BigDecimal("25")),
                        new AdjustmentRequest.AdjustmentLine(11L, new BigDecimal("-2"),
                                BigDecimal.ZERO)));

        var result = service.adjust(request);

        assertThat(result.totalIncrease()).isEqualByComparingTo("3");
        assertThat(result.totalDecrease()).isEqualByComparingTo("2");
        verify(ledger).increase(eq(41L), eq(10L), eq(7L), eq(8L),
                eq(new BigDecimal("3")), eq(new BigDecimal("25")),
                eq("ADJUSTMENT_IN"), eq("ADJUSTMENT"), isNull(), eq(10L),
                eq("ADJUSTMENT-key-1-1"), eq("Cycle correction"));
        verify(ledger).decrease(eq(41L), eq(11L), eq(7L), eq(8L),
                eq(new BigDecimal("2")), eq("ADJUSTMENT_OUT"), eq("ADJUSTMENT"),
                isNull(), eq(11L), eq("ADJUSTMENT-key-1-2"), eq("Cycle correction"));
        verify(ledger).audit(eq(41L), eq("ADJUSTMENT"), isNull(), eq("POSTED"),
                isNull(), anyMap());
    }

    @Test
    void duplicateProductAdjustmentIsRejectedBeforeSecondMutation() {
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(ledger.requireLocation(41L, 7L, null)).thenReturn(8L);
        AdjustmentRequest request = new AdjustmentRequest(7L, null, "Correction", "key-2",
                List.of(
                        new AdjustmentRequest.AdjustmentLine(10L, BigDecimal.ONE, BigDecimal.ZERO),
                        new AdjustmentRequest.AdjustmentLine(10L, BigDecimal.ONE, BigDecimal.ZERO)));

        assertThatThrownBy(() -> service.adjust(request)).isInstanceOf(ApiException.class);
        verify(ledger, times(1)).increase(anyLong(), anyLong(), anyLong(), anyLong(),
                any(), any(), anyString(), anyString(), any(), any(), anyString(), anyString());
        verify(ledger, never()).audit(anyLong(), anyString(), any(), anyString(), any(), any());
    }
}
