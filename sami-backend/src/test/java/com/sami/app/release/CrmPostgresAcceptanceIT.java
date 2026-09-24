package com.sami.app.release;

import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.crm.dto.CrmWorkflowDtos.FollowUpRequest;
import com.sami.app.crm.dto.CrmWorkflowDtos.LeadRequest;
import com.sami.app.crm.dto.CrmWorkflowDtos.OpportunityRequest;
import com.sami.app.crm.dto.CrmWorkflowDtos.OutcomeRequest;
import com.sami.app.crm.service.CrmWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/** Real PostgreSQL acceptance coverage for the additive Phase 5 CRM workflow foundation. */
class CrmPostgresAcceptanceIT extends PostgresApplicationFixture {
    @Autowired JdbcTemplate jdbc;
    @Autowired CrmWorkflowService workflow;
    @Autowired TenantContext tenants;
    @Autowired TransactionTemplate transactions;

    @Test
    void leadOpportunityFollowUpAndOutcomePersistWithinTenant() {
        long tenant = 1L;
        long customer = customer(tenant, "phase5-crm-customer");

        long leadId = tenants.callAsTenant(tenant, () -> transactions.execute(status ->
                workflow.createLead(new LeadRequest("Phase 5 lead", customer, "REFERRAL", null, "initial contact")).getId()));
        long opportunityId = tenants.callAsTenant(tenant, () -> transactions.execute(status ->
                workflow.createOpportunity(new OpportunityRequest("Phase 5 opportunity", customer, leadId, null,
                        java.time.LocalDate.now().plusDays(14), "qualified interest")).getId()));
        Instant due = Instant.parse("2026-11-01T10:00:00Z");
        long taskId = tenants.callAsTenant(tenant, () -> transactions.execute(status ->
                workflow.createTask(new FollowUpRequest(customer, leadId, opportunityId, null, due,
                        "phase5-sale-satisfaction-1")).getId()));

        long replayedTaskId = tenants.callAsTenant(tenant, () -> transactions.execute(status ->
                workflow.createTask(new FollowUpRequest(customer, leadId, opportunityId, null, due,
                        "phase5-sale-satisfaction-1")).getId()));
        assertEquals(taskId, replayedTaskId, "replaying an authoritative event must not create a second task");

        tenants.callAsTenant(tenant, () -> { transactions.executeWithoutResult(status ->
                workflow.completeTask(taskId, new OutcomeRequest("issue", "customer reported a product issue")));
            return null;
        });

        var lead = jdbc.queryForMap("select tenant_id, customer_id, status from crm_leads where id=?", leadId);
        assertEquals(tenant, ((Number) lead.get("tenant_id")).longValue());
        assertEquals(customer, ((Number) lead.get("customer_id")).longValue());
        assertEquals("OPEN", lead.get("status"));
        assertEquals(1, jdbc.queryForObject("select count(*) from crm_opportunities where tenant_id=? and id=? and customer_id=? and lead_id=?",
                Integer.class, tenant, opportunityId, customer, leadId));
        var task = jdbc.queryForMap("select tenant_id, customer_id, lead_id, opportunity_id, status, outcome, outcome_note from crm_follow_up_tasks where id=?",
                taskId);
        assertEquals(tenant, ((Number) task.get("tenant_id")).longValue());
        assertEquals(customer, ((Number) task.get("customer_id")).longValue());
        assertEquals(leadId, ((Number) task.get("lead_id")).longValue());
        assertEquals(opportunityId, ((Number) task.get("opportunity_id")).longValue());
        assertEquals("COMPLETED", task.get("status"));
        assertEquals("ISSUE", task.get("outcome"));
        assertEquals("customer reported a product issue", task.get("outcome_note"));
        assertEquals(1, jdbc.queryForObject("select count(*) from crm_follow_up_tasks where tenant_id=? and idempotency_key=?",
                Integer.class, tenant, "phase5-sale-satisfaction-1"));
    }

    @Test
    void crossTenantCustomerReferenceIsRejectedWithoutWorkflowMutation() {
        long tenant = 1L;
        Long otherTenant = jdbc.query("select id from tenants where id<>? order by id limit 1", rs -> rs.next() ? rs.getLong(1) : null, tenant);
        if (otherTenant == null) {
            otherTenant = jdbc.queryForObject("insert into tenants(code,name,status_id) select ?,?,id from licensing_statuses order by id limit 1 returning id",
                    Long.class, "P5-CRM-OTHER-" + System.nanoTime(), "P5 CRM Other Tenant");
        }
        long otherCustomer = customer(otherTenant, "phase5-cross-tenant-customer");
        assertThrows(RuntimeException.class, () -> tenants.callAsTenant(tenant, () -> transactions.execute(status ->
                workflow.createLead(new LeadRequest("cross tenant", otherCustomer, "REFERRAL", null, null)))));
        assertEquals(0, jdbc.queryForObject("select count(*) from crm_leads where tenant_id=? and title=?",
                Integer.class, tenant, "cross tenant"));
    }

    private long customer(long tenant, String code) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        long type = jdbc.queryForObject("select id from customer_types order by id limit 1", Long.class);
        long status = jdbc.queryForObject("select id from customer_statuses where is_default=true limit 1", Long.class);
        return jdbc.queryForObject("insert into customers(tenant_id,customer_code,display_name,type_id,status_id,created_at,updated_at) values(?,?,?,?,?,?,?) returning id",
                Long.class, tenant, code, code, type, status, now, now);
    }
}
