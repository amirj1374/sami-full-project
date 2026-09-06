package com.sami.app.organization.dto;
import jakarta.validation.constraints.NotNull;
import java.util.List;
public final class OrganizationGrantDtos {
 private OrganizationGrantDtos() {}
 public record AssignmentRequest(@NotNull Long userId,@NotNull Long companyId,@NotNull Long roleId) {}
 public record BranchGrantRequest(@NotNull Long branchId) {}
 public record AssignmentResponse(Long id,Long userId,Long companyId,Long roleId,boolean active,List<Long> branchIds) {}
}
