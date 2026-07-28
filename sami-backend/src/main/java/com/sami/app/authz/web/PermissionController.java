package com.sami.app.authz.web;

import com.sami.app.authz.dto.CreatePermissionRequest;
import com.sami.app.authz.dto.ModulePermissionsGroup;
import com.sami.app.authz.dto.PermissionResponse;
import com.sami.app.authz.dto.UpdatePermissionRequest;
import com.sami.app.authz.service.PermissionService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Permission administration. Every endpoint declares its required permission via
 * the {@code @authz} bean; the grouped listing also accepts {@code roles:view}
 * because the role permission matrix is built from it. Business guards (system
 * permissions cannot be deleted; action/code immutable) live in
 * {@link PermissionService}.
 */
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@Tag(name = "Permissions", description = "Permission administration")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @PreAuthorize("@authz.has('permissions:view')")
    @Operation(summary = "List permissions (paginated, optionally filtered by module)")
    public ApiResponse<PageResponse<PermissionResponse>> list(
            @RequestParam(required = false) Long moduleId,
            @PageableDefault(size = 20, sort = "code") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(permissionService.list(moduleId, pageable)));
    }

    @GetMapping("/grouped")
    @PreAuthorize("@authz.hasAny('permissions:view','roles:view')")
    @Operation(summary = "List every module (including disabled) with its permissions")
    public ApiResponse<List<ModulePermissionsGroup>> grouped() {
        return ApiResponse.ok(permissionService.grouped());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('permissions:create')")
    @Operation(summary = "Create a custom permission (code derived as <module>:<action>)")
    public ApiResponse<PermissionResponse> create(@Valid @RequestBody CreatePermissionRequest request) {
        return ApiResponse.ok(permissionService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.has('permissions:edit')")
    @Operation(summary = "Update a permission's name and description (action and code are immutable)")
    public ApiResponse<PermissionResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody UpdatePermissionRequest request) {
        return ApiResponse.ok(permissionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('permissions:delete')")
    @Operation(summary = "Delete a permission (system permissions are protected)")
    public void delete(@PathVariable Long id) {
        permissionService.delete(id);
    }
}
