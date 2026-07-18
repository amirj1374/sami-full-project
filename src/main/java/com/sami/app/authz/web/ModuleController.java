package com.sami.app.authz.web;

import com.sami.app.authz.dto.CreateModuleRequest;
import com.sami.app.authz.dto.ModuleResponse;
import com.sami.app.authz.dto.UpdateModuleRequest;
import com.sami.app.authz.dto.UpdateModuleStatusRequest;
import com.sami.app.authz.service.ModuleService;
import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Module administration. Every endpoint declares its required permission via the
 * {@code @authz} bean; business guards (system modules cannot be disabled or
 * deleted) live in {@link ModuleService}.
 */
@RestController
@RequestMapping("/api/v1/modules")
@RequiredArgsConstructor
@Tag(name = "Modules", description = "Module administration")
public class ModuleController {

    private final ModuleService moduleService;

    @GetMapping
    @PreAuthorize("@authz.has('modules:view')")
    @Operation(summary = "List modules (paginated)")
    public ApiResponse<PageResponse<ModuleResponse>> list(
            @PageableDefault(size = 20, sort = "displayOrder") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(moduleService.list(pageable)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('modules:create')")
    @Operation(summary = "Create a module, optionally with the six standard permissions")
    public ApiResponse<ModuleResponse> create(@Valid @RequestBody CreateModuleRequest request) {
        return ApiResponse.ok(moduleService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.has('modules:edit')")
    @Operation(summary = "Update a module (code is immutable)")
    public ApiResponse<ModuleResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody UpdateModuleRequest request) {
        return ApiResponse.ok(moduleService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@authz.has('modules:edit')")
    @Operation(summary = "Enable or disable a module (system modules cannot be disabled)")
    public ApiResponse<ModuleResponse> updateStatus(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateModuleStatusRequest request) {
        return ApiResponse.ok(moduleService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('modules:delete')")
    @Operation(summary = "Delete a module (system modules are protected; permissions cascade)")
    public void delete(@PathVariable Long id) {
        moduleService.delete(id);
    }
}
