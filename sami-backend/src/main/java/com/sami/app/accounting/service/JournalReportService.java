package com.sami.app.accounting.service;

import com.sami.app.common.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service @RequiredArgsConstructor
public class JournalReportService {
    private final JdbcTemplate jdbc;
    private final TenantContext tenants;

    @Transactional(readOnly = true)
    public List<Map<String,Object>> entries(int limit) {
        int safe = Math.min(Math.max(limit, 1), 100);
        return jdbc.queryForList("select e.id,e.posting_reference,e.description,e.currency_code,e.created_at,coalesce(sum(l.debit_amount),0) debit_total,coalesce(sum(l.credit_amount),0) credit_total from accounting_journal_entries e join accounting_journal_lines l on l.journal_entry_id=e.id where e.tenant_id=? group by e.id order by e.created_at desc,e.id desc limit ?", tenants.requireTenantId(), safe);
    }
}
