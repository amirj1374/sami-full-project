package com.sami.app.organization.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.organization.dto.BranchDtos.Request;
import com.sami.app.organization.dto.BranchDtos.Response;
import com.sami.app.organization.service.BranchService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organization/companies/{companyId}/branches")
@RequiredArgsConstructor
public class BranchController {
    private final BranchService service;
    @GetMapping @PreAuthorize("@authz.has('organization:view')")
    public ApiResponse<List<Response>> list(@PathVariable Long companyId) { return ApiResponse.ok(service.list(companyId)); }
    @PostMapping @PreAuthorize("@authz.has('organization:create')")
    public ApiResponse<Response> create(@PathVariable Long companyId, @Valid @RequestBody Request request) { return ApiResponse.ok(service.create(companyId, request)); }
    @PutMapping("/{id}") @PreAuthorize("@authz.has('organization:edit')")
    public ApiResponse<Response> update(@PathVariable Long companyId, @PathVariable Long id, @Valid @RequestBody Request request) { return ApiResponse.ok(service.update(companyId, id, request)); }
}
