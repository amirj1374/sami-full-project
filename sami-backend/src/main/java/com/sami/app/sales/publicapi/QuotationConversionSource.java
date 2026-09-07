package com.sami.app.sales.publicapi;

import java.math.BigDecimal;
import java.util.List;

/** Read-only commercial snapshot for a future Sales Order conversion. */
public interface QuotationConversionSource {
    QuotationSnapshot requireIssuedQuotation(Long quotationId);

    record QuotationSnapshot(Long quotationId, Long tenantId, Long companyId, Long branchId,
                             Long customerId, Long contactId, String currency, List<Line> lines) {}

    record Line(Long sourceLineId, Long productId, String productSku, String productName,
                BigDecimal quantity, BigDecimal unitPrice, BigDecimal discount, BigDecimal lineTotal) {}
}
