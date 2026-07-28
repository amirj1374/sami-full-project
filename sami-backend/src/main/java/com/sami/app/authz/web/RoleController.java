package com.sami.app.authz.web;

import com.sami.app.authz.dto.AssignPermissionsRequest;
import com.sami.app.authz.dto.CreateRoleRequest;
import com.sami.app.authz.dto.DuplicateRoleRequest;
import com.sami.app.authz.dto.RoleResponse;
import com.sami.app.authz.dto.UpdateRoleRequest;
import com.sami.app.authz.service.RoleService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Role administration. Every endpoint declares its required permission via the
 * {@code @authz} bean; business guards (system roles, roles in use, super-admin
 * permission set) live in {@link RoleService}.
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Role administration")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("@authz.has('roles:view')")
    @Operation(summary = "List roles (paginated)")
    public ApiResponse<PageResponse<RoleResponse>> list(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(roleService.list(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.has('roles:view')")
    @Operation(summary = "Get a role by id")
    public ApiResponse<RoleResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(roleService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('roles:create')")
    @Operation(summary = "Create a role")
    public ApiResponse<RoleResponse> create(@Valid @RequestBody CreateRoleRequest request) {
        return ApiResponse.ok(roleService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.has('roles:edit')")
    @Operation(summary = "Update a role (system roles cannot be renamed)")
    public ApiResponse<RoleResponse> update(@PathVariable Long id,
                                            @Valid @RequestBody UpdateRoleRequest request) {
        return ApiResponse.ok(roleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('roles:delete')")
    @Operation(summary = "Delete a role (system roles and roles in use are protected)")
    public void delete(@PathVariable Long id) {
        roleService.delete(id);
    }

    @PostMapping("/{id}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('roles:create')")
    @Operation(summary = "Duplicate a role under a new name, copying its permission set")
    public ApiResponse<RoleResponse> duplicate(@PathVariable Long id,
                                               @Valid @RequestBody DuplicateRoleRequest request) {
        return ApiResponse.ok(roleService.duplicate(id, request));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("@authz.has('roles:edit')")
    @Operation(summary = "Replace a role's permission set in full")
    public ApiResponse<RoleResponse> assignPermissions(@PathVariable Long id,
                                                       @Valid @RequestBody AssignPermissionsRequest request) {
        return ApiResponse.ok(roleService.assignPermissions(id, request));
    }
}
