package com.sami.app.sales.compatibility;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.contact.publicapi.ContactCustomerRoleLookup;
import com.sami.app.organization.service.FoundationAuditService;
import com.sami.app.organization.service.OrganizationScopeService;
import com.sami.app.sales.domain.LegacySaleDocumentMapping;
import com.sami.app.sales.domain.Sale;
import com.sami.app.sales.repository.LegacySaleDocumentMappingRepository;
import com.sami.app.sales.repository.SaleRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compatibility boundary for the pre-Phase-1 Sale aggregate.
 *
 * <p>Future Sales document services use this service instead of rewriting a
 * legacy Sale or its historical references. It is deliberately internal: the
 * existing {@code /v1/sales} API remains the legacy read API during dual-read.
 */
@Service
@RequiredArgsConstructor
public class LegacySaleCompatibilityService {

    private static final String MAPPING_REASON = "LEGACY_COMPATIBILITY";

    private final TenantContext tenantContext;
    private final ContactCustomerRoleLookup contacts;
    private final OrganizationScopeService organizationScope;
    private final SaleRepository sales;
    private final LegacySaleDocumentMappingRepository mappings;
    private final FoundationAuditService audit;

    @Transactional(readOnly = true)
    public LegacySaleReference requireReadableLegacySale(Long legacySaleId) {
        Sale sale = findScopedLegacySale(legacySaleId);
        return LegacySaleReference.from(sale, contacts.requireContactIdForCustomer(sale.getCustomerId()));
    }

    @Transactional(readOnly = true)
    public List<DocumentProvenance> documentProvenance(Long legacySaleId) {
        Sale sale = findScopedLegacySale(legacySaleId);
        return mappings.findByTenantIdAndLegacySaleIdOrderByDocumentTypeAscDocumentIdAsc(
                        sale.getTenantId(), sale.getId())
                .stream().map(DocumentProvenance::from).toList();
    }

    /**
     * Records a future document link without changing the legacy Sale. The
     * caller must supply the persisted document's scope; it must exactly match
     * the legacy Sale and is also protected by the database composite FK.
     */
    @Transactional
    public DocumentProvenance recordDocumentProvenance(DocumentReference reference) {
        Sale sale = findScopedLegacySale(reference.legacySaleId());
        contacts.requireContactIdForCustomer(sale.getCustomerId());
        if (!sale.getCompanyId().equals(reference.companyId()) || !sale.getBranchId().equals(reference.branchId())) {
            throw new ApiException(ErrorCode.ACCESS_DENIED,
                    "Future Sales document scope must match the legacy Sale");
        }
        String type = reference.documentType().name();
        if (mappings.existsByTenantIdAndLegacySaleIdAndDocumentTypeAndDocumentId(
                sale.getTenantId(), sale.getId(), type, reference.documentId())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "Legacy Sale provenance is already recorded for this document");
        }
        LegacySaleDocumentMapping mapping = mappings.save(LegacySaleDocumentMapping.builder()
                .tenantId(sale.getTenantId())
                .legacySale(sale)
                .companyId(sale.getCompanyId())
                .branchId(sale.getBranchId())
                .documentType(type)
                .documentId(reference.documentId())
                .mappingReason(MAPPING_REASON)
                .build());
        audit.record(sale.getTenantId(), sale.getCompanyId(), sale.getBranchId(),
                "SALES_LEGACY_COMPATIBILITY", sale.getId(), "DOCUMENT_PROVENANCE_RECORDED",
                MAPPING_REASON, java.util.Map.of("documentType", type, "documentId", reference.documentId()));
        return DocumentProvenance.from(mapping);
    }

    private Sale findScopedLegacySale(Long legacySaleId) {
        if (legacySaleId == null || legacySaleId <= 0) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Legacy Sale id is required");
        }
        Long tenant = tenantContext.requireTenantId();
        Sale sale = sales.findByIdAndTenantId(legacySaleId, tenant)
                .orElseThrow(() -> ResourceNotFoundException.of("Legacy Sale", legacySaleId));
        organizationScope.requireScope(sale.getCompanyId(), sale.getBranchId());
        return sale;
    }

    public enum FutureSalesDocumentType {
        QUOTATION, SALES_ORDER, FULFILLMENT, SALES_INVOICE
    }

    public record DocumentReference(Long legacySaleId, Long companyId, Long branchId,
                                    FutureSalesDocumentType documentType, Long documentId) {
        public DocumentReference {
            if (companyId == null || companyId <= 0 || branchId == null || branchId <= 0
                    || documentType == null || documentId == null || documentId <= 0) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Document provenance requires a scoped future Sales document");
            }
        }
    }

    public record LegacySaleReference(Long legacySaleId, Long tenantId, Long companyId,
                                      Long branchId, Long customerId, Long contactId, String invoiceNumber,
                                      String status) {
        static LegacySaleReference from(Sale sale, Long contactId) {
            return new LegacySaleReference(sale.getId(), sale.getTenantId(), sale.getCompanyId(),
                    sale.getBranchId(), sale.getCustomerId(), contactId, sale.getInvoiceNumber(), sale.getStatus().name());
        }
    }

    public record DocumentProvenance(Long id, Long legacySaleId, Long companyId,
                                     Long branchId, String documentType, Long documentId,
                                     String mappingReason) {
        static DocumentProvenance from(LegacySaleDocumentMapping mapping) {
            return new DocumentProvenance(mapping.getId(), mapping.getLegacySale().getId(),
                    mapping.getCompanyId(), mapping.getBranchId(),
                    mapping.getDocumentType().toUpperCase(Locale.ROOT), mapping.getDocumentId(),
                    mapping.getMappingReason());
        }
    }
}
