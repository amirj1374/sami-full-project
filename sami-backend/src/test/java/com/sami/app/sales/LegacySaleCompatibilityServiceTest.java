package com.sami.app.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.contact.publicapi.ContactCustomerRoleLookup;
import com.sami.app.organization.service.FoundationAuditService;
import com.sami.app.organization.service.OrganizationScopeService;
import com.sami.app.sales.compatibility.LegacySaleCompatibilityService;
import com.sami.app.sales.compatibility.LegacySaleCompatibilityService.DocumentReference;
import com.sami.app.sales.compatibility.LegacySaleCompatibilityService.FutureSalesDocumentType;
import com.sami.app.sales.domain.LegacySaleDocumentMapping;
import com.sami.app.sales.domain.Sale;
import com.sami.app.sales.domain.SaleStatus;
import com.sami.app.sales.repository.LegacySaleDocumentMappingRepository;
import com.sami.app.sales.repository.SaleRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LegacySaleCompatibilityServiceTest {

    @Mock TenantContext tenantContext;
    @Mock ContactCustomerRoleLookup contacts;
    @Mock OrganizationScopeService organizationScope;
    @Mock SaleRepository sales;
    @Mock LegacySaleDocumentMappingRepository mappings;
    @Mock FoundationAuditService audit;
    @InjectMocks LegacySaleCompatibilityService service;

    @Test
    void readableLegacySalePreservesHistoricalIdentifiersWithoutMutation() {
        Sale sale = legacySale();
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(sales.findByIdAndTenantId(91L, 41L)).thenReturn(Optional.of(sale));
        when(contacts.requireContactIdForCustomer(12L)).thenReturn(73L);

        var reference = service.requireReadableLegacySale(91L);

        assertThat(reference.legacySaleId()).isEqualTo(91L);
        assertThat(reference.tenantId()).isEqualTo(41L);
        assertThat(reference.companyId()).isEqualTo(7L);
        assertThat(reference.branchId()).isEqualTo(8L);
        assertThat(reference.customerId()).isEqualTo(12L);
        assertThat(reference.contactId()).isEqualTo(73L);
        assertThat(reference.invoiceNumber()).isEqualTo("SAL-2026-000091");
        verify(organizationScope).requireScope(7L, 8L);
        verify(sales, never()).save(any());
        verifyNoInteractions(mappings, audit);
    }

    @Test
    void documentProvenanceLinksLegacySaleWithoutChangingIt() {
        Sale sale = legacySale();
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(sales.findByIdAndTenantId(91L, 41L)).thenReturn(Optional.of(sale));
        when(contacts.requireContactIdForCustomer(12L)).thenReturn(73L);
        when(mappings.existsByTenantIdAndLegacySaleIdAndDocumentTypeAndDocumentId(41L, 91L, "SALES_ORDER", 500L))
                .thenReturn(false);
        when(mappings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.recordDocumentProvenance(new DocumentReference(91L, 7L, 8L,
                FutureSalesDocumentType.SALES_ORDER, 500L));

        ArgumentCaptor<LegacySaleDocumentMapping> mapping = ArgumentCaptor.forClass(LegacySaleDocumentMapping.class);
        verify(mappings).save(mapping.capture());
        assertThat(mapping.getValue().getLegacySale()).isSameAs(sale);
        assertThat(mapping.getValue().getTenantId()).isEqualTo(41L);
        assertThat(mapping.getValue().getCompanyId()).isEqualTo(7L);
        assertThat(mapping.getValue().getBranchId()).isEqualTo(8L);
        assertThat(mapping.getValue().getDocumentType()).isEqualTo("SALES_ORDER");
        assertThat(mapping.getValue().getDocumentId()).isEqualTo(500L);
        verify(organizationScope).requireScope(7L, 8L);
        verify(audit).record(eq(41L), eq(7L), eq(8L), eq("SALES_LEGACY_COMPATIBILITY"), eq(91L),
                eq("DOCUMENT_PROVENANCE_RECORDED"), eq("LEGACY_COMPATIBILITY"), anyMap());
        verify(sales, never()).save(any());
    }

    @Test
    void alteredFutureDocumentScopeIsDeniedBeforeWritingProvenance() {
        when(tenantContext.requireTenantId()).thenReturn(41L);
        when(sales.findByIdAndTenantId(91L, 41L)).thenReturn(Optional.of(legacySale()));

        assertThatThrownBy(() -> service.recordDocumentProvenance(new DocumentReference(91L, 99L, 8L,
                FutureSalesDocumentType.SALES_INVOICE, 501L))).isInstanceOf(ApiException.class);

        verify(organizationScope).requireScope(7L, 8L);
        verifyNoInteractions(mappings, audit);
    }

    private Sale legacySale() {
        Sale sale = Sale.builder().tenantId(41L).companyId(7L).branchId(8L).customerId(12L)
                .sellerId(15L).invoiceNumber("SAL-2026-000091").saleType("RETAIL")
                .status(SaleStatus.COMPLETED).currency("IRR").build();
        ReflectionTestUtils.setField(sale, "id", 91L);
        return sale;
    }
}
