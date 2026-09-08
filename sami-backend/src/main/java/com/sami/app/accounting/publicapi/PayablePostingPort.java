package com.sami.app.accounting.publicapi;
import java.math.BigDecimal; import java.time.Instant;
public interface PayablePostingPort { String createPayable(PayableCommand command); record PayableCommand(Long tenantId,Long companyId,Long branchId,Long supplierId,Long supplierInvoiceId,BigDecimal amount,String currency,Instant issuedAt,String idempotencyKey) {}
}
