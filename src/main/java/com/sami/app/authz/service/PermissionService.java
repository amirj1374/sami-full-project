package com.sami.app.authz.service;

import com.sami.app.authz.domain.AppModule;
import com.sami.app.authz.domain.Permission;
import com.sami.app.authz.dto.CreatePermissionRequest;
import com.sami.app.authz.dto.ModulePermissionsGroup;
import com.sami.app.authz.dto.PermissionResponse;
import com.sami.app.authz.dto.UpdatePermissionRequest;
import com.sami.app.authz.repository.AppModuleRepository;
import com.sami.app.authz.repository.PermissionRepository;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Permission administration use-cases.
 *
 * <p>The permission code is always derived server-side as
 * {@code <module.code>:<action>}; action and code are immutable after creation
 * because granted authorities and {@code @PreAuthorize} rules reference the
 * code. System permissions (the seeded standard actions) cannot be deleted.
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final AppModuleRepository moduleRepository;

    @Transactional(readOnly = true)
    public Page<PermissionResponse> list(Long moduleId, Pageable pageable) {
        Page<Permission> page = moduleId == null
                ? permissionRepository.findAll(pageable)
                : permissionRepository.findByModuleId(moduleId, pageable);
        return page.map(PermissionResponse::from);
    }

    /**
     * Every module (including disabled ones) with its permissions, ordered by
     * display order — the role matrix must show everything grantable.
     */
    @Transactional(readOnly = true)
    public List<ModulePermissionsGroup> grouped() {
        Map<Long, List<PermissionResponse>> byModuleId = permissionRepository.findAllByOrderByActionAsc().stream()
                .collect(Collectors.groupingBy(permission -> permission.getModule().getId(),
                        Collectors.mapping(PermissionResponse::from, Collectors.toList())));
        return moduleRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(module -> ModulePermissionsGroup.from(module,
                        byModuleId.getOrDefault(module.getId(), List.of())))
                .toList();
    }

    @Transactional
    public PermissionResponse create(CreatePermissionRequest request) {
        AppModule module = moduleRepository.findById(request.moduleId())
                .orElseThrow(() -> ResourceNotFoundException.of("Module", request.moduleId()));
        if (permissionRepository.existsByModuleIdAndAction(module.getId(), request.action())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "Action '%s' already exists for module '%s'".formatted(request.action(), module.getCode()));
        }

        Permission permission = Permission.builder()
                .module(module)
                .action(request.action())
                .code(module.getCode() + ":" + request.action())
                .name(request.name())
                .description(request.description())
                .build();

        return PermissionResponse.from(permissionRepository.save(permission));
    }

    @Transactional
    public PermissionResponse update(Long id, UpdatePermissionRequest request) {
        Permission permission = findOrThrow(id);
        // Only name/description are editable (on any permission, system included);
        // action and code are structurally immutable — the request cannot carry them.
        permission.setName(request.name());
        permission.setDescription(request.description());
        return PermissionResponse.from(permission);
    }

    @Transactional
    public void delete(Long id) {
        Permission permission = findOrThrow(id);
        if (permission.isSystem()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "System permissions cannot be deleted");
        }
        // Database FKs cascade any role grants of this permission.
        permissionRepository.delete(permission);
    }

    private Permission findOrThrow(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Permission", id));
    }
}
