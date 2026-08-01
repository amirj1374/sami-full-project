package com.sami.app.crm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.crm.domain.Segment;
import com.sami.app.crm.dto.SegmentDtos.SegmentRequest;
import com.sami.app.crm.repository.SegmentRepository;
import com.sami.app.crm.service.CustomerService;
import com.sami.app.crm.service.SegmentService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SegmentTenantTest {

    @Test
    void createChecksUniquenessAndPersistsWithinTrustedTenant() {
        SegmentRepository segments = mock(SegmentRepository.class);
        CustomerService customers = mock(CustomerService.class);
        TenantContext tenantContext = mock(TenantContext.class);
        SegmentService service = new SegmentService(segments, customers, new ObjectMapper(), tenantContext);
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(segments.existsByTenantIdAndNameIgnoreCase(41L, "Inactive customers")).thenReturn(false);
        when(segments.save(any())).thenAnswer(call -> call.getArgument(0));

        service.create(new SegmentRequest("Inactive customers", null, Map.of("noEventSinceDays", 90)));

        var captor = org.mockito.ArgumentCaptor.forClass(Segment.class);
        verify(segments).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(41L);
        verify(segments).existsByTenantIdAndNameIgnoreCase(41L, "Inactive customers");
    }
}
