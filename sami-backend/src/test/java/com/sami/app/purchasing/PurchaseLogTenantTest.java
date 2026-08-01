package com.sami.app.purchasing;

import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.purchasing.domain.Purchase;
import com.sami.app.purchasing.domain.PurchaseLog;
import com.sami.app.purchasing.event.PurchaseDomainEvent;
import com.sami.app.purchasing.repository.PurchaseLogRepository;
import com.sami.app.purchasing.service.PurchaseLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseLogTenantTest {

    @Mock TenantContext tenantContext;
    @Mock PurchaseLogRepository logs;
    @Mock ApplicationEventPublisher events;
    @InjectMocks PurchaseLogService service;

    @Test
    void auditRowAndDomainEventUseAggregateTenant() {
        Purchase purchase = org.mockito.Mockito.spy(Purchase.builder()
                .tenantId(41L).purchaseNumber("PUR-TEST-1").build());
        org.mockito.Mockito.doReturn(7L).when(purchase).getId();
        when(logs.save(any())).thenAnswer(call -> call.getArgument(0));

        service.record(purchase, PurchaseLogService.APPROVED, "Approved", null);

        ArgumentCaptor<PurchaseLog> row = ArgumentCaptor.forClass(PurchaseLog.class);
        verify(logs).save(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(41L);

        ArgumentCaptor<PurchaseDomainEvent> event = ArgumentCaptor.forClass(PurchaseDomainEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().tenantId()).isEqualTo(41L);
        assertThat(event.getValue().purchaseId()).isEqualTo(7L);
    }
}
