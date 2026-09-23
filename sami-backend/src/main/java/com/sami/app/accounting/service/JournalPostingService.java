package com.sami.app.accounting.service;

import com.sami.app.accounting.publicapi.JournalPostingPort;
import com.sami.app.accounting.publicapi.JournalPostingPort.JournalCommand;
import com.sami.app.accounting.publicapi.JournalPostingPort.JournalLine;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;

/** Transactional Accounting-owned posting boundary. */
@Service
@RequiredArgsConstructor
public class JournalPostingService implements JournalPostingPort {
    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public long post(JournalCommand c) {
        if (!"IRR".equalsIgnoreCase(c.currencyCode())) {
            throw new IllegalArgumentException("Only whole-Toman IRR posting is currently supported");
        }
        if (new HashSet<Integer>(c.lines().stream().map(JournalLine::lineNumber).toList()).size() != c.lines().size()) {
            throw new IllegalArgumentException("Journal line numbers must be unique");
        }
        BigDecimal debit = c.lines().stream().map(JournalLine::debit).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = c.lines().stream().map(JournalLine::credit).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (debit.signum() <= 0 || debit.compareTo(credit) != 0) {
            throw new IllegalArgumentException("Journal must be non-zero and balanced");
        }
        Long existing = jdbc.query("select id from accounting_journal_entries where tenant_id=? and idempotency_key=? for update",
                rs -> rs.next() ? rs.getLong(1) : null, c.tenantId(), c.idempotencyKey());
        if (existing != null) return existing;
        Long open = jdbc.query("select id from accounting_fiscal_periods where id=? and tenant_id=? and company_id=? and branch_id=? and status='OPEN' for update",
                rs -> rs.next() ? rs.getLong(1) : null, c.fiscalPeriodId(), c.tenantId(), c.companyId(), c.branchId());
        if (open == null) throw new IllegalStateException("Fiscal period is not open for posting");
        for (JournalLine line : c.lines()) {
            Integer valid = jdbc.queryForObject("select count(*) from accounting_accounts where id=? and tenant_id=? and company_id=? and branch_id=? and postable=true and active=true",
                    Integer.class, line.accountId(), c.tenantId(), c.companyId(), c.branchId());
            if (valid == null || valid != 1) throw new IllegalArgumentException("Journal account is not valid for this scope");
        }
        Long id = jdbc.queryForObject("insert into accounting_journal_entries(tenant_id,company_id,branch_id,fiscal_period_id,entry_type,posting_reference,idempotency_key,currency_code,description,created_by) values(?,?,?,?,?,?,?,?,?,?) returning id",
                Long.class, c.tenantId(), c.companyId(), c.branchId(), c.fiscalPeriodId(), "ORIGINAL", c.postingReference(), c.idempotencyKey(), "IRR", c.description(), c.createdBy());
        for (JournalLine line : c.lines()) {
            jdbc.update("insert into accounting_journal_lines(journal_entry_id,tenant_id,account_id,line_number,debit_amount,credit_amount,description) values(?,?,?,?,?,?,?)",
                    id, c.tenantId(), line.accountId(), line.lineNumber(), line.debit(), line.credit(), line.description());
        }
        return id;
    }
}
