package com.sami.app.accounting.publicapi;
import java.math.BigDecimal;
import java.time.Instant;
/** Canonical Accounting boundary for applying a customer receipt to receivables. */
public interface ReceivableSettlementPort {
 String settle(ReceiptSettlementCommand command);
 record ReceiptSettlementCommand(Long tenantId, Long companyId, Long branchId, Long customerId, Long invoiceId, BigDecimal amount, String currency, Instant settledAt, String idempotencyKey) { }
}
