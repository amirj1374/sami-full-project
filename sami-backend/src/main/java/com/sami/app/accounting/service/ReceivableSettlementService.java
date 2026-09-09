package com.sami.app.accounting.service;

import com.sami.app.accounting.publicapi.ReceivableSettlementPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;

@Service
@RequiredArgsConstructor
public class ReceivableSettlementService implements ReceivableSettlementPort {
    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public String settle(ReceiptSettlementCommand c) {
        var existing = jdbc.query("select posting_reference from accounting_receivable_settlements where tenant_id=? and idempotency_key=?", (rs, n) -> rs.getString(1), c.tenantId(), c.idempotencyKey());
        if (!existing.isEmpty()) return existing.getFirst();
        String ref = "SET-" + c.tenantId() + "-" + c.idempotencyKey();
        jdbc.update("insert into accounting_receivable_settlements(tenant_id,company_id,branch_id,customer_id,invoice_id,amount,currency,settled_at,idempotency_key,posting_reference) values(?,?,?,?,?,?,?,?,?,?)", c.tenantId(), c.companyId(), c.branchId(), c.customerId(), c.invoiceId(), c.amount(), c.currency(), Timestamp.from(c.settledAt()), c.idempotencyKey(), ref);
        return ref;
    }
}
