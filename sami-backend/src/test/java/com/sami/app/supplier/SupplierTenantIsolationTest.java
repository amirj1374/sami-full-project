package com.sami.app.supplier.service;

import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.supplier.SupplierProperties;
import com.sami.app.supplier.domain.Supplier;
import com.sami.app.supplier.domain.SupDuplicateRule;
import com.sami.app.supplier.dto.SupplierDtos.SupplierFilter;
import com.sami.app.supplier.dto.SupplierDtos.SupplierRequest;
import com.sami.app.supplier.repository.SupCategoryRepository;
import com.sami.app.supplier.repository.SupDuplicateRuleRepository;
import com.sami.app.supplier.repository.SupPaymentTermRepository;
import com.sami.app.supplier.repository.SupStatusRepository;
import com.sami.app.supplier.repository.SupTagRepository;
import com.sami.app.supplier.repository.SupTypeRepository;
import com.sami.app.supplier.repository.SupplierRepository;
import com.sami.app.supplier.repository.SupplierSpecifications;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierTenantIsolationTest {

    @Mock SupplierRepository suppliers;
    @Mock SupTypeRepository types;
    @Mock SupStatusRepository statuses;
    @Mock SupPaymentTermRepository paymentTerms;
    @Mock SupTagRepository tags;
    @Mock SupCategoryRepository categories;
    @Mock SupDuplicateRuleRepository duplicateRules;
    @Mock SupplierProperties properties;
    @Mock SupLogService logs;
    @Mock TenantContext tenants;
    @InjectMocks SupplierService service;

    @Test
    void detailLookupUsesTrustedTenantAndHidesAnotherTenantsSupplier() {
        when(tenants.requireTenantId()).thenReturn(41L);
        when(suppliers.findWithDetailsByIdAndTenantId(9L, 41L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(9L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(suppliers).findWithDetailsByIdAndTenantId(9L, 41L);
    }

    @Test
    void detailLookupReturnsLegitimateSameTenantSupplier() {
        Supplier supplier = Supplier.builder().tenantId(41L).supplierCode("SUP-41").build();
        when(tenants.requireTenantId()).thenReturn(41L);
        when(suppliers.findWithDetailsByIdAndTenantId(9L, 41L)).thenReturn(Optional.of(supplier));

        assertThat(service.findWithDetailsOrThrow(9L)).isSameAs(supplier);
    }

    @SuppressWarnings("unchecked")
    @Test
    void listAndExportAlwaysApplyCurrentTenantBoundary() {
        when(tenants.requireTenantId()).thenReturn(41L);
        when(suppliers.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(Page.empty());
        when(suppliers.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of());
        SupplierFilter filter = new SupplierFilter(
                null, null, null, null, null, null, null, null, null, null, true);

        service.list(filter, PageRequest.of(0, 20));
        service.exportCsv(filter);

        var specificationCaptor = org.mockito.ArgumentCaptor.forClass(Specification.class);
        verify(suppliers).findAll(specificationCaptor.capture(), any(PageRequest.class));
        verify(suppliers).findAll(specificationCaptor.capture(), any(Sort.class));
        specificationCaptor.getAllValues().forEach(this::assertTenantPredicate);
    }

    @SuppressWarnings("unchecked")
    @Test
    void duplicateDetectionIsScopedToCurrentTenant() throws Exception {
        SupDuplicateRule rule = SupDuplicateRule.builder()
                .identifier(SupDuplicateRule.Identifier.TAX_NUMBER).enabled(true).build();
        when(duplicateRules.findByEnabledTrue()).thenReturn(List.of(rule));
        when(tenants.requireTenantId()).thenReturn(41L);
        when(suppliers.findAll(any(Specification.class))).thenReturn(List.of());
        SupplierRequest request = new SupplierRequest(
                "Company", "Supplier", null, null, null, " TAX-1 ", null, null,
                null, null, null, null, null, null, 1L, null, null, null,
                null, null, null, null, null, null, false, null);

        Method method = SupplierService.class.getDeclaredMethod(
                "enforceDuplicatePolicy", SupplierRequest.class, Long.class);
        method.setAccessible(true);
        method.invoke(service, request, null);

        var specificationCaptor = org.mockito.ArgumentCaptor.forClass(Specification.class);
        verify(suppliers).findAll(specificationCaptor.capture());
        assertTenantPredicate(specificationCaptor.getValue());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void listSpecificationRequiresTenantPredicate() {
        Root<Supplier> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path<Object> tenantPath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        when(root.get("tenantId")).thenReturn(tenantPath);
        when(builder.equal(tenantPath, 41L)).thenReturn(predicate);

        assertThat(SupplierSpecifications.hasTenant(41L).toPredicate(root, query, builder))
                .isSameAs(predicate);
        verify(builder).equal(tenantPath, 41L);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void assertTenantPredicate(Specification<Supplier> specification) {
        Root<Supplier> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path<Object> tenantPath = mock(Path.class);
        Predicate tenantPredicate = mock(Predicate.class);
        when(root.get("tenantId")).thenReturn(tenantPath);
        when(builder.equal(tenantPath, 41L)).thenReturn(tenantPredicate);

        specification.toPredicate(root, query, builder);

        verify(builder).equal(tenantPath, 41L);
    }
}
