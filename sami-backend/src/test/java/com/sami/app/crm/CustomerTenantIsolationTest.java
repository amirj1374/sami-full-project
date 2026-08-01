package com.sami.app.crm;

import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.storage.FileStorage;
import com.sami.app.common.storage.StorageProperties;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.crm.repository.CustomerRepository;
import com.sami.app.crm.repository.CustomerSourceRepository;
import com.sami.app.crm.repository.CustomerStatusRepository;
import com.sami.app.crm.repository.CustomerTagRepository;
import com.sami.app.crm.repository.CustomerTypeRepository;
import com.sami.app.crm.service.CustomerEventService;
import com.sami.app.crm.service.CustomerService;
import com.sami.app.crm.service.DuplicateDetectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerTenantIsolationTest {

    @Mock CustomerRepository customers;
    @Mock CustomerTypeRepository types;
    @Mock CustomerStatusRepository statuses;
    @Mock CustomerSourceRepository sources;
    @Mock CustomerTagRepository tags;
    @Mock DuplicateDetectionService duplicates;
    @Mock CustomerEventService events;
    @Mock FileStorage fileStorage;
    @Mock StorageProperties storageProperties;
    @Mock CrmProperties properties;
    @Mock TenantContext tenantContext;
    @InjectMocks CustomerService service;

    @Test
    void detailLookupUsesTrustedTenantAndHidesAnotherTenantsCustomer() {
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(customers.findWithDetailsByIdAndTenantId(9L, 41L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(9L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(customers).findWithDetailsByIdAndTenantId(9L, 41L);
    }

    @Test
    void detailGraphDoesNotJoinFetchBothListCollections() throws NoSuchMethodException {
        var method = CustomerRepository.class.getMethod(
                "findWithDetailsByIdAndTenantId", Long.class, Long.class);
        var graph = method.getAnnotation(org.springframework.data.jpa.repository.EntityGraph.class);

        assertThat(graph).isNotNull();
        assertThat(graph.attributePaths()).doesNotContain("contacts", "addresses");
    }
}
