package com.sami.app.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

public final class CrmWorkflowDtos {
    private CrmWorkflowDtos() {}
    public record LeadRequest(@NotBlank @Size(max=160) String title, Long customerId,
                              @Size(max=64) String source, Long assignedUserId,
                              @Size(max=4000) String notes) {}
    public record OpportunityRequest(@NotBlank @Size(max=160) String title, Long customerId,
                              Long leadId, Long assignedUserId, LocalDate expectedCloseDate,
                              @Size(max=4000) String notes) {}
    public record FollowUpRequest(Long customerId, Long leadId, Long opportunityId,
                              Long assignedUserId, Instant dueAt, String idempotencyKey) {}
    public record OutcomeRequest(@NotBlank String outcome, @Size(max=4000) String note) {}
}
