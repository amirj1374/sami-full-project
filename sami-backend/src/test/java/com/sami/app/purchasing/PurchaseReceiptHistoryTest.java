package com.sami.app.purchasing;

import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.crm.service.CustomerEventService;
import com.sami.app.hamta.HamtaService;
import com.sami.app.inventory.publicapi.InventoryStockOperations;
import com.sami.app.purchasing.domain.PurchaseReceipt;
import com.sami.app.purchasing.domain.PurchaseReceiptItem;
import com.sami.app.purchasing.domain.PurchaseUnitIdentifier;
import com.sami.app.purchasing.dto.PurchaseDtos.ReceiptResponse;
import com.sami.app.purchasing.repository.PurchaseReceiptRepository;
import com.sami.app.purchasing.repository.PurchaseUnitIdentifierRepository;
import com.sami.app.purchasing.service.PurchaseLogService;
import com.sami.app.purchasing.service.PurchaseReceivingService;
import com.sami.app.purchasing.service.PurchaseService;
import com.sami.app.purchasing.service.PurchasingConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseReceiptHistoryTest {

    @Mock PurchaseService purchases;
    @Mock PurchaseReceiptRepository receipts;
    @Mock PurchasingConfigService config;
    @Mock PurchaseUnitIdentifierRepository identifiers;
    @Mock TenantContext tenants;
    @Mock PurchaseLogService logs;
    @Mock InventoryStockOperations inventory;
    @Mock CustomerEventService customerEvents;
    @Mock HamtaService hamta;

    @Test
    void historyUsesSeparateIdentifierReadForMultipleReceiptsAndSerializedRows() {
        PurchaseReceipt first = mock(PurchaseReceipt.class);
        PurchaseReceipt second = mock(PurchaseReceipt.class);
        PurchaseReceiptItem firstItem = mock(PurchaseReceiptItem.class);
        PurchaseReceiptItem secondItem = mock(PurchaseReceiptItem.class);
        PurchaseUnitIdentifier firstIdentifier = mock(PurchaseUnitIdentifier.class);
        PurchaseUnitIdentifier secondIdentifier = mock(PurchaseUnitIdentifier.class);

        when(tenants.requireTenantId()).thenReturn(52L);
        when(first.getId()).thenReturn(20L);
        when(second.getId()).thenReturn(10L);
        when(first.getItems()).thenReturn(List.of(firstItem));
        when(second.getItems()).thenReturn(List.of(secondItem));
        when(firstItem.getId()).thenReturn(201L);
        when(secondItem.getId()).thenReturn(101L);
        when(firstItem.getQuantity()).thenReturn(BigDecimal.ONE);
        when(secondItem.getQuantity()).thenReturn(BigDecimal.ONE);
        when(firstItem.getPurchaseItem()).thenReturn(mock(com.sami.app.purchasing.domain.PurchaseItem.class));
        when(secondItem.getPurchaseItem()).thenReturn(mock(com.sami.app.purchasing.domain.PurchaseItem.class));
        when(firstItem.getPurchaseItem().getId()).thenReturn(2001L);
        when(secondItem.getPurchaseItem().getId()).thenReturn(1001L);
        when(firstIdentifier.getReceiptItem()).thenReturn(firstItem);
        when(secondIdentifier.getReceiptItem()).thenReturn(secondItem);
        when(firstIdentifier.getUnitIndex()).thenReturn(1);
        when(secondIdentifier.getUnitIndex()).thenReturn(1);
        when(firstIdentifier.getValue()).thenReturn("SERIAL-2");
        when(secondIdentifier.getValue()).thenReturn("SERIAL-1");
        var type = mock(com.sami.app.purchasing.domain.PurIdentifierType.class);
        when(type.getName()).thenReturn("Serial");
        when(firstIdentifier.getIdentifierType()).thenReturn(type);
        when(secondIdentifier.getIdentifierType()).thenReturn(type);
        when(receipts.findByPurchaseIdAndTenantIdOrderByCreatedAtDesc(7L, 52L))
                .thenReturn(List.of(first, second));
        when(identifiers.findByReceiptItemReceiptIdInAndTenantIdOrderByReceiptItemIdAscUnitIndexAscIdAsc(
                List.of(20L, 10L), 52L)).thenReturn(List.of(secondIdentifier, firstIdentifier));

        PurchaseReceivingService service = new PurchaseReceivingService(purchases, receipts, config,
                identifiers, tenants, logs, inventory, customerEvents, hamta);
        List<ReceiptResponse> result = service.history(7L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).lines().getFirst().identifiers().getFirst().value()).isEqualTo("SERIAL-2");
        assertThat(result.get(1).lines().getFirst().identifiers().getFirst().value()).isEqualTo("SERIAL-1");
        verify(receipts).findByPurchaseIdAndTenantIdOrderByCreatedAtDesc(7L, 52L);
        verify(identifiers).findByReceiptItemReceiptIdInAndTenantIdOrderByReceiptItemIdAscUnitIndexAscIdAsc(
                List.of(20L, 10L), 52L);
    }
}
