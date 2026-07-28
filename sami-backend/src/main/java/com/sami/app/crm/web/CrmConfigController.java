package com.sami.app.crm.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.crm.dto.CustomerSourceResponse;
import com.sami.app.crm.dto.CustomerStatusResponse;
import com.sami.app.crm.dto.CustomerTagResponse;
import com.sami.app.crm.dto.CustomerTypeResponse;
import com.sami.app.crm.dto.LookupRequests.DuplicateRulesRequest;
import com.sami.app.crm.dto.LookupRequests.SourceRequest;
import com.sami.app.crm.dto.LookupRequests.StatusRequest;
import com.sami.app.crm.dto.LookupRequests.TagRequest;
import com.sami.app.crm.dto.LookupRequests.TypeRequest;
import com.sami.app.crm.service.CrmConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * The CRM's configuration surface: customer types, statuses, sources, tags and
 * duplicate rules — everything an administrator tunes without code changes.
 * Reads need {@code customers:view} (forms render the lookups); writes need
 * {@code customers:edit}.
 */
@RestController
@RequestMapping("/api/v1/crm")
@RequiredArgsConstructor
@Tag(name = "CRM configuration", description = "Configurable CRM lookup data and rules")
public class CrmConfigController {

    private final CrmConfigService config;

    // ----------------------------------------------------------------- types

    @GetMapping("/types")
    @PreAuthorize("@authz.has('customers:view')")
    @Operation(summary = "List customer types")
    public ApiResponse<List<CustomerTypeResponse>> listTypes() {
        return ApiResponse.ok(config.listTypes());
    }

    @PostMapping("/types")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasAny('customers:manage-config','customers:edit')")
    @Operation(summary = "Create a customer type")
    public ApiResponse<CustomerTypeResponse> createType(@Valid @RequestBody TypeRequest request) {
        return ApiResponse.ok(config.createType(request));
    }

    @PutMapping("/types/{id}")
    @PreAuthorize("@authz.hasAny('customers:manage-config','customers:edit')")
    @Operation(summary = "Update a customer type")
    public ApiResponse<CustomerTypeResponse> updateType(@PathVariable Long id,
                                                        @Valid @RequestBody TypeRequest request) {
        return ApiResponse.ok(config.updateType(id, request));
    }

    @DeleteMapping("/types/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.hasAny('customers:manage-config','customers:edit')")
    @Operation(summary = "Delete a customer type (blocked for system/in-use entries)")
    public void deleteType(@PathVariable Long id) {
        config.deleteType(id);
    }

    // -------------------------------------------------------------- statuses

    @GetMapping("/statuses")
    @PreAuthorize("@authz.has('customers:view')")
    @Operation(summary = "List customer statuses")
    public ApiResponse<List<CustomerStatusResponse>> listStatuses() {
        return ApiResponse.ok(config.listStatuses());
    }

    @PostMapping("/statuses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasAny('customers:manage-config','customers:edit')")
    @Operation(summary = "Create a customer status")
    public ApiResponse<CustomerStatusResponse> createStatus(@Valid @RequestBody StatusRequest request) {
        return ApiResponse.ok(config.createStatus(request));
    }

    @PutMapping("/statuses/{id}")
    @PreAuthorize("@authz.hasAny('customers:manage-config','customers:edit')")
    @Operation(summary = "Update a customer status")
    public ApiResponse<CustomerStatusResponse> updateStatus(@PathVariable Long id,
                                                            @Valid @RequestBody StatusRequest request) {
        return ApiResponse.ok(config.updateStatus(id, request));
    }

    @DeleteMapping("/statuses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.hasAny('customers:manage-config','customers:edit')")
    @Operation(summary = "Delete a customer status (blocked for system/in-use entries)")
    public void deleteStatus(@PathVariable Long id) {
        config.deleteStatus(id);
    }

    // --------------------------------------------------------------- sources

    @GetMapping("/sources")
    @PreAuthorize("@authz.has('customers:view')")
    @Operation(summary = "List acquisition sources")
    public ApiResponse<List<CustomerSourceResponse>> listSources() {
        return ApiResponse.ok(config.listSources());
    }

    @PostMapping("/sources")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasAny('customers:manage-config','customers:edit')")
    @Operation(summary = "Create an acquisition source")
    public ApiResponse<CustomerSourceResponse> createSource(@Valid @RequestBody SourceRequest request) {
        return ApiResponse.ok(config.createSource(request));
    }

    @PutMapping("/sources/{id}")
    @PreAuthorize("@authz.hasAny('customers:manage-config','customers:edit')")
    @Operation(summary = "Update an acquisition source")
    public ApiResponse<CustomerSourceResponse> updateSource(@PathVariable Long id,
                                                            @Valid @RequestBody SourceRequest request) {
        return ApiResponse.ok(config.updateSource(id, request));
    }

    @DeleteMapping("/sources/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.hasAny('customers:manage-config','customers:edit')")
    @Operation(summary = "Delete an acquisition source (blocked for system/in-use entries)")
    public void deleteSource(@PathVariable Long id) {
        config.deleteSource(id);
    }

    // ------------------------------------------------------------------ tags

    @GetMapping("/tags")
    @PreAuthorize("@authz.has('customers:view')")
    @Operation(summary = "List tags")
    public ApiResponse<List<CustomerTagResponse>> listTags() {
        return ApiResponse.ok(config.listTags());
    }

    @PostMapping("/tags")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.hasAny('customers:manage-tags','customers:edit')")
    @Operation(summary = "Create a tag")
    public ApiResponse<CustomerTagResponse> createTag(@Valid @RequestBody TagRequest request) {
        return ApiResponse.ok(config.createTag(request));
    }

    @PutMapping("/tags/{id}")
    @PreAuthorize("@authz.hasAny('customers:manage-tags','customers:edit')")
    @Operation(summary = "Update a tag")
    public ApiResponse<CustomerTagResponse> updateTag(@PathVariable Long id,
                                                      @Valid @RequestBody TagRequest request) {
        return ApiResponse.ok(config.updateTag(id, request));
    }

    @DeleteMapping("/tags/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.hasAny('customers:manage-tags','customers:edit')")
    @Operation(summary = "Delete a tag (unlinks it from all customers)")
    public void deleteTag(@PathVariable Long id) {
        config.deleteTag(id);
    }

    // ------------------------------------------------------- duplicate rules

    @GetMapping("/duplicate-rules")
    @PreAuthorize("@authz.has('customers:view')")
    @Operation(summary = "Current duplicate-detection rule toggles")
    public ApiResponse<Map<String, Boolean>> duplicateRules() {
        return ApiResponse.ok(config.listDuplicateRules());
    }

    @PutMapping("/duplicate-rules")
    @PreAuthorize("@authz.hasAny('customers:manage-config','customers:edit')")
    @Operation(summary = "Toggle duplicate-detection identifiers (takes effect immediately)")
    public ApiResponse<Map<String, Boolean>> updateDuplicateRules(
            @Valid @RequestBody DuplicateRulesRequest request) {
        return ApiResponse.ok(config.updateDuplicateRules(request));
    }
}
