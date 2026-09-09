package com.sami.app.release;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import com.sami.app.sales.document.*;
import com.sami.app.sales.order.*;
import com.sami.app.sales.delivery.*;
import com.sami.app.sales.invoice.*;
import com.sami.app.sales.receipt.*;
import java.math.BigDecimal;
import java.util.List;
import static com.sami.app.sales.document.SalesDocumentType.QUOTATION;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import com.sami.app.organization.service.OrganizationScopeService;
import com.sami.app.organization.dto.OrganizationContextDtos;
import com.sami.app.organization.dto.CompanyDtos.CompanyRequest;
import com.sami.app.organization.dto.BranchDtos;
import com.sami.app.organization.dto.OrganizationGrantDtos.AssignmentRequest;
import com.sami.app.organization.dto.OrganizationGrantDtos.BranchGrantRequest;

/** PostgreSQL acceptance entry point; the reusable fixture is intentionally the single bootstrap owner. */
class SalesReceiptPostgresAcceptanceIT extends SalesBusinessFixturePostgresIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired SalesDocumentService quotation;
    @Autowired SalesOrderService orders;
    @Autowired DeliveryService deliveries;
    @Autowired SalesInvoiceService invoices;
    @Autowired SalesReceiptService receipts;
    @Autowired ReceiptConfirmationOrchestrator confirmations;
    @SpyBean com.sami.app.treasury.service.TreasuryService treasuryBoundary;
    @SpyBean com.sami.app.accounting.service.ReceivableSettlementService accountingBoundary;
    @Autowired OrganizationScopeService organizationScope;

    @AfterEach void resetFailureBoundaries() {
        reset(treasuryBoundary, accountingBoundary);
    }

    @Test
    void reusableSalesFixturePersistsReceiptPrerequisites() {
        var c = chain(new BigDecimal("100.00"));
        var confirmed = confirmations.confirm(c.receiptId(), c.data().treasuryAccountId());
        assertEquals("CONFIRMED", confirmed.get("status"));
        assertEquals(1, jdbc.queryForObject("select count(*) from accounting_receivable_settlements where tenant_id=? and amount=100", Integer.class, c.data().tenantId()));
        assertEquals(1, jdbc.queryForObject("select count(*) from foundation_audit_logs where tenant_id=? and subject_type='SALES_RECEIPT' and subject_id=? and action='RECEIPT_CONFIRMED'", Integer.class, c.data().tenantId(), c.receiptId()));
        assertEquals(1, jdbc.queryForObject("select count(*) from treasury_transactions where tenant_id=? and reference_module='sales-receipt'", Integer.class, c.data().tenantId()));
        assertEquals(1, jdbc.queryForObject("select count(*) from accounting_receivable_settlements where tenant_id=? and posting_reference is not null", Integer.class, c.data().tenantId()));
    }

    @Test void partialAllocationSettlesOnlyAllocatedAmount() {
        var c = chain(new BigDecimal("40.00"));
        var confirmed = confirmations.confirm(c.receiptId(), c.data().treasuryAccountId());
        assertEquals("CONFIRMED", confirmed.get("status"));
        assertEquals(40, jdbc.queryForObject("select amount from accounting_receivable_settlements where tenant_id=? order by id desc limit 1", BigDecimal.class, c.data().tenantId()).intValue());
        assertEquals(40, jdbc.queryForObject("select amount from treasury_transactions where tenant_id=? order by id desc limit 1", BigDecimal.class, c.data().tenantId()).intValue());
    }

    @Test void overAllocationIsRejectedBeforeConfirmation() {
        var c = chain(new BigDecimal("100.00"));
        assertThrows(RuntimeException.class, () -> receipts.allocate(c.receiptId(), c.invoiceId(), new BigDecimal("101.00")));
        assertEquals(0, jdbc.queryForObject("select count(*) from accounting_receivable_settlements where tenant_id=? and idempotency_key like ?", Integer.class, c.data().tenantId(), "SALES-RECEIPT-%"));
    }

    @Test void duplicateConfirmationIsIdempotent() {
        var c = chain(new BigDecimal("100.00"));
        confirmations.confirm(c.receiptId(), c.data().treasuryAccountId());
        var second = confirmations.confirm(c.receiptId(), c.data().treasuryAccountId());
        assertEquals("CONFIRMED", second.get("status"));
        assertEquals(1, jdbc.queryForObject("select count(*) from accounting_receivable_settlements where tenant_id=? and amount=100", Integer.class, c.data().tenantId()));
    }

    @Test void wrongTreasuryAccountIsRejectedWithoutFinancialSideEffects() {
        var c = chain(new BigDecimal("100.00"));
        assertThrows(RuntimeException.class, () -> confirmations.confirm(c.receiptId(), c.data().treasuryAccountId() + 999999L));
        assertEquals(0, jdbc.queryForObject("select count(*) from accounting_receivable_settlements where tenant_id=?", Integer.class, c.data().tenantId()));
        assertEquals(0, jdbc.queryForObject("select count(*) from treasury_transactions where tenant_id=? and reference_module='sales-receipt'", Integer.class, c.data().tenantId()));
    }

    @Test void crossTenantLookupCannotSeeReceipt() {
        var c = chain(new BigDecimal("100.00"));
        assertThrows(RuntimeException.class, () -> jdbc.queryForMap("select * from sales_receipts where tenant_id=? and id=?", c.data().tenantId() + 999999L, c.receiptId()));
    }

    @Test void treasuryFailureRollsBackConfirmation() {
        var c = chain(new BigDecimal("100.00"));
        doThrow(new IllegalStateException("treasury unavailable")).when(treasuryBoundary)
                .recordInflow(org.mockito.ArgumentMatchers.eq(c.data().treasuryAccountId()), org.mockito.ArgumentMatchers.eq(new BigDecimal("100.00")), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("sales-receipt"), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        assertThrows(IllegalStateException.class, () -> confirmations.confirm(c.receiptId(), c.data().treasuryAccountId()));
        assertEquals("DRAFT", jdbc.queryForObject("select status from sales_receipts where tenant_id=? and id=?", String.class, c.data().tenantId(), c.receiptId()));
        assertEquals(0, jdbc.queryForObject("select count(*) from accounting_receivable_settlements where tenant_id=?", Integer.class, c.data().tenantId()));
    }

    @Test void accountingFailureRollsBackTreasuryAndConfirmation() {
        var c = chain(new BigDecimal("100.00"));
        doThrow(new IllegalStateException("accounting unavailable")).when(accountingBoundary).settle(org.mockito.ArgumentMatchers.any());
        assertThrows(IllegalStateException.class, () -> confirmations.confirm(c.receiptId(), c.data().treasuryAccountId()));
        assertEquals("DRAFT", jdbc.queryForObject("select status from sales_receipts where tenant_id=? and id=?", String.class, c.data().tenantId(), c.receiptId()));
        assertEquals(0, jdbc.queryForObject("select count(*) from accounting_receivable_settlements where tenant_id=?", Integer.class, c.data().tenantId()));
        assertEquals(0, jdbc.queryForObject("select count(*) from treasury_transactions where tenant_id=? and reference_module='sales-receipt'", Integer.class, c.data().tenantId()));
    }

    @Test void crossCompanyConfirmationIsRejectedByRealScope() {
        var c = chain(new BigDecimal("100.00"));
        Long role = jdbc.queryForObject("select id from roles order by id limit 1", Long.class);
        var company = companies.create(new CompanyRequest("OTHER-COMPANY-" + c.data().tenantId(), "Other Company", null, null, null, "IRR", "Asia/Tehran", "fa", 2, null, null, null, null, null, null, null, null, true, 0, null));
        Long bt = jdbc.queryForObject("select id from branch_types where is_default and tenant_id is null limit 1", Long.class);
        var branch = branches.create(company.id(), new BranchDtos.Request("OTHER-BRANCH-" + c.data().tenantId(), "Other Branch", bt, true, 2, null));
        var a = grants.assign(new AssignmentRequest(c.data().userId(), company.id(), role)); grants.grant(a.id(), new BranchGrantRequest(branch.id()));
        grants.list(c.data().userId()).stream().filter(x -> x.companyId().equals(c.data().companyId())).forEach(x -> x.branchIds().forEach(b -> grants.revokeBranch(x.id(), b)));
        organizationScope.select(new OrganizationContextDtos.SelectRequest(company.id(), branch.id()));
        assertThrows(RuntimeException.class, () -> confirmations.confirm(c.receiptId(), c.data().treasuryAccountId()));
        assertEquals("DRAFT", jdbc.queryForObject("select status from sales_receipts where id=?", String.class, c.receiptId()));
    }

    @Test void crossBranchConfirmationIsRejectedByRealScope() {
        var c = chain(new BigDecimal("100.00"));
        Long role = jdbc.queryForObject("select id from roles order by id limit 1", Long.class);
        Long bt = jdbc.queryForObject("select id from branch_types where is_default and tenant_id is null limit 1", Long.class);
        var branch = branches.create(c.data().companyId(), new BranchDtos.Request("OTHER-BRANCH-" + c.data().tenantId(), "Other Branch", bt, true, 2, null));
        var a = grants.assign(new AssignmentRequest(c.data().userId(), c.data().companyId(), role)); grants.grant(a.id(), new BranchGrantRequest(branch.id()));
        grants.list(c.data().userId()).stream().filter(x -> x.branchIds().contains(c.data().branchId())).forEach(x -> grants.revokeBranch(x.id(), c.data().branchId()));
        organizationScope.select(new OrganizationContextDtos.SelectRequest(c.data().companyId(), branch.id()));
        assertThrows(RuntimeException.class, () -> confirmations.confirm(c.receiptId(), c.data().treasuryAccountId()));
        assertEquals("DRAFT", jdbc.queryForObject("select status from sales_receipts where id=?", String.class, c.receiptId()));
    }

    @Test void crossTenantConfirmationIsRejectedByTenantScope() {
        var c = chain(new BigDecimal("100.00"));
        SalesSecurityTestSupport.authenticateTenantUser(1L, c.data().tenantId() + 100000L, "tenant-b@sami.test", "sales:confirm");
        assertThrows(RuntimeException.class, () -> confirmations.confirm(c.receiptId(), c.data().treasuryAccountId()));
        assertEquals("DRAFT", jdbc.queryForObject("select status from sales_receipts where id=?", String.class, c.receiptId()));
    }

    private Chain chain(BigDecimal receiptAmount) {
        SalesBusinessFixturePostgresIT.Data d = super.bootstrap();
        var q = quotation.create(new SalesDocumentDtos.Request(d.companyId(), d.branchId(), d.customerId(), QUOTATION, "IRR", null,
                List.of(new SalesDocumentDtos.LineRequest(d.productId(), BigDecimal.ONE, new BigDecimal("100.00"), BigDecimal.ZERO)), null));
        quotation.issue(q.id());
        var order = orders.convert(q.id()); orders.confirm(order.id());
        var delivery = deliveries.create(order.id(), new DeliveryDtos.Request(d.companyId(), d.branchId(), null,
                List.of(new DeliveryDtos.LineRequest(order.lines().getFirst().id(), BigDecimal.ONE)))); deliveries.confirm(delivery.id());
        var invoice = invoices.create(new SalesInvoiceDtos.Request(order.id(), d.companyId(), d.branchId(), "IRR", null,
                List.of(new SalesInvoiceDtos.LineRequest(delivery.lines().getFirst().id(), BigDecimal.ONE)))); invoices.issue(invoice.id());
        var receipt = receipts.create(d.companyId(), d.branchId(), d.customerId(), receiptAmount, "CASH", null, "E2E-" + d.tenantId() + "-" + receiptAmount);
        Long receiptId = ((Number) receipt.get("id")).longValue(); receipts.allocate(receiptId, invoice.id(), receiptAmount);
        return new Chain(d, invoice.id(), receiptId);
    }

    private record Chain(SalesBusinessFixturePostgresIT.Data data, Long invoiceId, Long receiptId) { }
}

