package com.sami.app.accounting.service;

import com.sami.app.accounting.publicapi.ReceivablePostingPort;
import com.sami.app.accounting.publicapi.ReceivablePostingPort.SalesInvoiceReceivableCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;

/** Accounting-owned persistence boundary for issued sales receivables. */
@Service
@RequiredArgsConstructor
public class ReceivablePostingService implements ReceivablePostingPort {
    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void createReceivable(SalesInvoiceReceivableCommand c) {
        Integer existing = jdbc.queryForObject("select count(*) from accounting_receivables where tenant_id=? and idempotency_key=?",
                Integer.class, c.tenantId(), c.idempotencyKey());
        if (existing != null && existing > 0) return;
        jdbc.update("insert into accounting_receivables(tenant_id,company_id,branch_id,counterparty_contact_id,sales_invoice_id,amount,currency,issued_at,idempotency_key) values(?,?,?,?,?,?,?,?,?)",
                c.tenantId(), c.companyId(), c.branchId(), c.counterpartyContactId(), c.salesInvoiceId(), c.amount(), c.currency(), Timestamp.from(c.issuedAt()), c.idempotencyKey());
    }
}
