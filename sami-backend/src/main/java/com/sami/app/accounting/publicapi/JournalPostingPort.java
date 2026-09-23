package com.sami.app.accounting.publicapi;

import java.math.BigDecimal;
import java.util.List;

/** Canonical Accounting posting boundary for balanced, idempotent journals. */
public interface JournalPostingPort {
    long post(JournalCommand command);

    record JournalCommand(Long tenantId, Long companyId, Long branchId, Long fiscalPeriodId,
                          String postingReference, String idempotencyKey, String currencyCode,
                          String description, List<JournalLine> lines, Long createdBy,
                          Long reversalOfId) {
        public JournalCommand {
            if (tenantId == null || tenantId <= 0 || companyId == null || companyId <= 0
                    || branchId == null || branchId <= 0 || fiscalPeriodId == null || fiscalPeriodId <= 0
                    || postingReference == null || postingReference.isBlank()
                    || idempotencyKey == null || idempotencyKey.isBlank()
                    || lines == null || lines.isEmpty()) {
                throw new IllegalArgumentException("A scoped journal with posting and idempotency references is required");
            }
            lines = List.copyOf(lines);
        }
    }

    record JournalLine(Long accountId, int lineNumber, BigDecimal debit, BigDecimal credit, String description) {
        public JournalLine {
            if (accountId == null || accountId <= 0 || lineNumber <= 0) {
                throw new IllegalArgumentException("A journal line requires an account and positive line number");
            }
            boolean hasDebit = debit != null && debit.signum() > 0;
            boolean hasCredit = credit != null && credit.signum() > 0;
            if (hasDebit == hasCredit) {
                throw new IllegalArgumentException("A journal line must contain exactly one positive debit or credit");
            }
        }
    }
}
