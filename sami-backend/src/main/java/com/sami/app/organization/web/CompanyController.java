package com.sami.app.organization.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.organization.dto.CompanyDtos.CompanyRequest;
import com.sami.app.organization.dto.CompanyDtos.CompanyResponse;
import com.sami.app.organization.service.CompanyService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organization/companies")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyService service;

    @GetMapping
    @PreAuthorize("@authz.has('organization:view')")
    public ApiResponse<List<CompanyResponse>> list() { return ApiResponse.ok(service.list()); }

    @PostMapping
    @PreAuthorize("@authz.has('organization:create')")
    public ApiResponse<CompanyResponse> create(@Valid @RequestBody CompanyRequest request) { return ApiResponse.ok(service.create(request)); }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.has('organization:edit')")
    public ApiResponse<CompanyResponse> update(@PathVariable Long id, @Valid @RequestBody CompanyRequest request) { return ApiResponse.ok(service.update(id, request)); }
}
