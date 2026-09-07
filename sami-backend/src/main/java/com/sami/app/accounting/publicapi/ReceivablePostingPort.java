package com.sami.app.accounting.publicapi;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Future Accounting-owned integration point for a finalized Sales Invoice.
 *
 * <p>This Slice defines no Accounting implementation and creates no ledger
 * entries. A later Invoice workflow may depend on this port instead of writing
 * a Sales-local general ledger.
 */
public interface ReceivablePostingPort {

    void createReceivable(SalesInvoiceReceivableCommand command);

    record SalesInvoiceReceivableCommand(Long tenantId, Long companyId, Long branchId,
                                         Long counterpartyContactId, Long salesInvoiceId,
                                         BigDecimal amount, String currency, Instant issuedAt,
                                         String idempotencyKey) {
        public SalesInvoiceReceivableCommand {
            if (tenantId == null || tenantId <= 0 || companyId == null || companyId <= 0
                    || branchId == null || branchId <= 0 || counterpartyContactId == null
                    || counterpartyContactId <= 0 || salesInvoiceId == null || salesInvoiceId <= 0
                    || amount == null || amount.signum() <= 0 || currency == null || currency.isBlank()
                    || issuedAt == null || idempotencyKey == null || idempotencyKey.isBlank()) {
                throw new IllegalArgumentException("A scoped, idempotent Sales Invoice receivable is required");
            }
        }
    }
}
