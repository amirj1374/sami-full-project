package com.sami.app.contact.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.contact.dto.ContactDtos.ContactResponse;
import com.sami.app.contact.dto.ContactDtos.LegacyMappingResponse;
import com.sami.app.contact.service.ContactQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
public class ContactController {
    private final ContactQueryService service;

    @GetMapping
    @PreAuthorize("@authz.has('customers:view')")
    public ApiResponse<List<ContactResponse>> list() { return ApiResponse.ok(service.list()); }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.has('customers:view')")
    public ApiResponse<ContactResponse> get(@PathVariable Long id) { return ApiResponse.ok(service.get(id)); }

    @GetMapping("/{id}/mappings")
    @PreAuthorize("@authz.has('customers:view')")
    public ApiResponse<List<LegacyMappingResponse>> mappings(@PathVariable Long id) { return ApiResponse.ok(service.mappings(id)); }
}
