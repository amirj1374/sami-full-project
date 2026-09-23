package com.sami.app.crm.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.crm.domain.*;
import com.sami.app.crm.dto.CrmWorkflowDtos.*;
import com.sami.app.crm.service.CrmWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/crm/workflows") @RequiredArgsConstructor
public class CrmWorkflowController {
    private final CrmWorkflowService service;
    @GetMapping("/leads") @PreAuthorize("@authz.has('customers:view')") public ApiResponse<List<CrmLead>> leads(){return ApiResponse.ok(service.leads());}
    @PostMapping("/leads") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@authz.has('customers:edit')") public ApiResponse<CrmLead> lead(@Valid @RequestBody LeadRequest r){return ApiResponse.ok(service.createLead(r));}
    @GetMapping("/opportunities") @PreAuthorize("@authz.has('customers:view')") public ApiResponse<List<CrmOpportunity>> opportunities(){return ApiResponse.ok(service.opportunities());}
    @PostMapping("/opportunities") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@authz.has('customers:edit')") public ApiResponse<CrmOpportunity> opportunity(@Valid @RequestBody OpportunityRequest r){return ApiResponse.ok(service.createOpportunity(r));}
    @GetMapping("/follow-ups") @PreAuthorize("@authz.has('customers:view')") public ApiResponse<List<CrmFollowUpTask>> tasks(){return ApiResponse.ok(service.tasks());}
    @PostMapping("/follow-ups") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@authz.has('customers:edit')") public ApiResponse<CrmFollowUpTask> task(@RequestBody FollowUpRequest r){return ApiResponse.ok(service.createTask(r));}
    @PostMapping("/follow-ups/{id}/complete") @PreAuthorize("@authz.has('customers:edit')") public ApiResponse<CrmFollowUpTask> complete(@PathVariable Long id,@Valid @RequestBody OutcomeRequest r){return ApiResponse.ok(service.completeTask(id,r));}
}
