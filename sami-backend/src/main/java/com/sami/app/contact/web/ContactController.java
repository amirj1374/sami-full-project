package com.sami.app.contact.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.contact.dto.ContactDtos.ContactResponse;
import com.sami.app.contact.dto.ContactDtos.LegacyMappingResponse;
import com.sami.app.contact.dto.ContactDtos.MergeResponse;
import com.sami.app.contact.dto.ContactDtos.RoleRequest;
import com.sami.app.contact.service.ContactQueryService;
import com.sami.app.contact.service.ContactWriteService;
import com.sami.app.contact.service.ContactReconciliationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
public class ContactController {
    private final ContactQueryService service;
    private final ContactWriteService writes;
    private final ContactReconciliationService reconciliation;

    @GetMapping
    @PreAuthorize("@authz.has('customers:view')")
    public ApiResponse<List<ContactResponse>> list() { return ApiResponse.ok(service.list()); }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.has('customers:view')")
    public ApiResponse<ContactResponse> get(@PathVariable Long id) { return ApiResponse.ok(service.get(id)); }

    @GetMapping("/{id}/mappings")
    @PreAuthorize("@authz.has('customers:view')")
    public ApiResponse<List<LegacyMappingResponse>> mappings(@PathVariable Long id) { return ApiResponse.ok(service.mappings(id)); }

    @GetMapping("/reconciliation")
    @PreAuthorize("@authz.has('customers:view')")
    public ApiResponse<java.util.Map<String, Object>> reconciliation() { return ApiResponse.ok(reconciliation.summary()); }

    @PostMapping("/{id}/roles")
    @PreAuthorize("@authz.has('customers:edit')")
    public ApiResponse<Void> addRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) { writes.addRole(id, request); return ApiResponse.ok(); }

    @PostMapping("/{sourceId}/merge-into/{targetId}")
    @PreAuthorize("@authz.has('customers:merge')")
    public ApiResponse<MergeResponse> merge(@PathVariable Long sourceId, @PathVariable Long targetId) { return ApiResponse.ok(writes.merge(sourceId, targetId)); }
}
