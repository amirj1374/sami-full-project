package com.sami.app.crm;

import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.crm.domain.Customer;
import com.sami.app.crm.domain.CustomerEvent;
import com.sami.app.crm.event.CustomerDomainEvent;
import com.sami.app.crm.repository.CustomerEventRepository;
import com.sami.app.crm.repository.CustomerRepository;
import com.sami.app.crm.service.CustomerEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerEventTenantTest {

    @Mock TenantContext tenantContext;
    @Mock CustomerEventRepository events;
    @Mock CustomerRepository customers;
    @Mock ApplicationEventPublisher publisher;
    @InjectMocks CustomerEventService service;

    @Test
    void auditRowAndDomainEventUseTrustedTenant() {
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(customers.findByIdAndTenantId(7L, 41L))
                .thenReturn(Optional.of(Customer.builder().tenantId(41L).build()));
        when(events.save(any())).thenAnswer(call -> call.getArgument(0));

        service.record(7L, CustomerEventService.UPDATED, "Updated", null);

        ArgumentCaptor<CustomerEvent> row = ArgumentCaptor.forClass(CustomerEvent.class);
        verify(events).save(row.capture());
        assertThat(row.getValue().getTenantId()).isEqualTo(41L);

        ArgumentCaptor<CustomerDomainEvent> domainEvent = ArgumentCaptor.forClass(CustomerDomainEvent.class);
        verify(publisher).publishEvent(domainEvent.capture());
        assertThat(domainEvent.getValue().tenantId()).isEqualTo(41L);
        assertThat(domainEvent.getValue().customerId()).isEqualTo(7L);
    }
}
