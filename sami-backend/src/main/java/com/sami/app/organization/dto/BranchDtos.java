package com.sami.app.organization.dto;

import com.sami.app.organization.domain.Branch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class BranchDtos {
    private BranchDtos() { }
    public record Request(@NotBlank @Size(max = 64) String code, @NotBlank @Size(max = 255) String name,
                          @NotNull Long branchTypeId, Boolean active, Integer displayOrder, Long expectedVersion) { }
    public record Response(Long id, Long companyId, Long branchTypeId, String code, String name, boolean active,
                           int displayOrder, Long version, Instant createdAt, Instant updatedAt) {
        public static Response from(Branch branch) {
            return new Response(branch.getId(), branch.getCompanyId(), branch.getBranchTypeId(), branch.getCode(),
                    branch.getName(), branch.isActive(), branch.getDisplayOrder(), branch.getVersion(),
                    branch.getCreatedAt(), branch.getUpdatedAt());
        }
    }
}
