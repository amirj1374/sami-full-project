package com.sami.app.sales.publicapi;

import java.math.BigDecimal;
import java.util.List;
import com.sami.app.sales.document.SalesDocumentType;

/** Read-only commercial snapshot for a future Sales Order conversion. */
public interface QuotationConversionSource {
    QuotationSnapshot requireIssuedQuotation(Long quotationId);

    record QuotationSnapshot(Long quotationId, SalesDocumentType documentType, Long tenantId, Long companyId,
                             Long branchId, Long customerId, Long contactId, String currency, List<Line> lines) {}

    record Line(Long sourceLineId, Long productId, String productSku, String productName,
                BigDecimal quantity, BigDecimal unitPrice, BigDecimal discount, BigDecimal lineTotal) {}
}
