package com.sami.app.organization.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class OrganizationContextDtos {
    private OrganizationContextDtos() { }
    public record CompanyChoice(Long id, String code, String name) { }
    public record BranchChoice(Long id, Long companyId, String code, String name) { }
    public record ContextResponse(Long companyId, Long branchId, List<CompanyChoice> companies, List<BranchChoice> branches) { }
    public record SelectRequest(@NotNull Long companyId, Long branchId) { }
}
