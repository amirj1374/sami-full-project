package com.sami.app.organization.web;
import com.sami.app.common.api.ApiResponse; import com.sami.app.organization.dto.OrganizationGrantDtos.*; import com.sami.app.organization.service.OrganizationGrantService; import jakarta.validation.Valid; import java.util.List; import lombok.RequiredArgsConstructor; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/organization/grants") @RequiredArgsConstructor public class OrganizationGrantController {
 private final OrganizationGrantService service;
 @GetMapping("/users/{userId}") @PreAuthorize("@authz.has('organization:view')") public ApiResponse<List<AssignmentResponse>> list(@PathVariable Long userId){return ApiResponse.ok(service.list(userId));}
 @PostMapping @PreAuthorize("@authz.has('organization:edit')") public ApiResponse<AssignmentResponse> assign(@Valid @RequestBody AssignmentRequest r){return ApiResponse.ok(service.assign(r));}
 @PostMapping("/{assignmentId}/branches") @PreAuthorize("@authz.has('organization:edit')") public ApiResponse<AssignmentResponse> grant(@PathVariable Long assignmentId,@Valid @RequestBody BranchGrantRequest r){return ApiResponse.ok(service.grant(assignmentId,r));}
 @DeleteMapping("/{assignmentId}/branches/{branchId}") @PreAuthorize("@authz.has('organization:edit')") public ApiResponse<Void> revokeBranch(@PathVariable Long assignmentId,@PathVariable Long branchId){service.revokeBranch(assignmentId,branchId);return ApiResponse.ok();}
 @DeleteMapping("/{assignmentId}") @PreAuthorize("@authz.has('organization:edit')") public ApiResponse<Void> revoke(@PathVariable Long assignmentId){service.revokeAssignment(assignmentId);return ApiResponse.ok();}
}
