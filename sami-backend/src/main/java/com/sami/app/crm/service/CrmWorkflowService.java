package com.sami.app.crm.service;

import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.crm.domain.*;
import com.sami.app.crm.dto.CrmWorkflowDtos.*;
import com.sami.app.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CrmWorkflowService {
    private final TenantContext tenants;
    private final CustomerService customers;
    private final CrmLeadRepository leads;
    private final CrmOpportunityRepository opportunities;
    private final CrmFollowUpTaskRepository tasks;
    private final CustomerEventService events;

    @Transactional(readOnly=true) public List<CrmLead> leads() { return leads.findByTenantIdOrderByCreatedAtDesc(tenants.requireTenantId()); }
    @Transactional public CrmLead createLead(LeadRequest r) { Long t=tenants.requireTenantId(); customer(r.customerId()); CrmLead x=new CrmLead(); x.setTenantId(t); x.setCustomerId(r.customerId()); x.setTitle(r.title().trim()); x.setSource(r.source()); x.setAssignedUserId(r.assignedUserId()); x.setNotes(r.notes()); return leads.save(x); }
    @Transactional(readOnly=true) public List<CrmOpportunity> opportunities() { return opportunities.findByTenantIdOrderByCreatedAtDesc(tenants.requireTenantId()); }
    @Transactional public CrmOpportunity createOpportunity(OpportunityRequest r) { Long t=tenants.requireTenantId(); customer(r.customerId()); if(r.leadId()!=null) lead(r.leadId()); CrmOpportunity x=new CrmOpportunity(); x.setTenantId(t); x.setCustomerId(r.customerId()); x.setLeadId(r.leadId()); x.setTitle(r.title().trim()); x.setAssignedUserId(r.assignedUserId()); x.setExpectedCloseDate(r.expectedCloseDate()); x.setNotes(r.notes()); return opportunities.save(x); }
    @Transactional(readOnly=true) public List<CrmFollowUpTask> tasks() { return tasks.findByTenantIdOrderByDueAtAsc(tenants.requireTenantId()); }
    @Transactional public CrmFollowUpTask createTask(FollowUpRequest r) { Long t=tenants.requireTenantId(); customer(r.customerId()); if(r.leadId()!=null) lead(r.leadId()); if(r.opportunityId()!=null) opportunity(r.opportunityId()); if(r.idempotencyKey()!=null) { var old=tasks.findByTenantIdAndIdempotencyKey(t,r.idempotencyKey()); if(old.isPresent()) return old.get(); } CrmFollowUpTask x=new CrmFollowUpTask(); x.setTenantId(t); x.setCustomerId(r.customerId()); x.setLeadId(r.leadId()); x.setOpportunityId(r.opportunityId()); x.setAssignedUserId(r.assignedUserId()); x.setDueAt(r.dueAt()); x.setIdempotencyKey(r.idempotencyKey()); return tasks.save(x); }
    @Transactional public CrmFollowUpTask completeTask(Long id, OutcomeRequest r) { CrmFollowUpTask x=task(id); x.setStatus("COMPLETED"); x.setOutcome(r.outcome().trim().toUpperCase()); x.setOutcomeNote(r.note()); x.setCompletedAt(Instant.now()); if (x.getCustomerId()!=null) events.record(x.getCustomerId(), "FOLLOW_UP_" + x.getOutcome(), "Follow-up completed", java.util.Map.of("note", x.getOutcomeNote() == null ? "" : x.getOutcomeNote()), "CRM"); return x; }
    private void customer(Long id) { if(id!=null) customers.getDetail(id); }
    private CrmLead lead(Long id) { return leads.findByIdAndTenantId(id,tenants.requireTenantId()).orElseThrow(()->ResourceNotFoundException.of("Lead",id)); }
    private CrmOpportunity opportunity(Long id) { return opportunities.findByIdAndTenantId(id,tenants.requireTenantId()).orElseThrow(()->ResourceNotFoundException.of("Opportunity",id)); }
    private CrmFollowUpTask task(Long id) { return tasks.findByIdAndTenantId(id,tenants.requireTenantId()).orElseThrow(()->ResourceNotFoundException.of("Follow-up task",id)); }
}
