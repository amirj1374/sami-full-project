package com.sami.app.authz.service;

import com.sami.app.authz.domain.Permission;
import com.sami.app.authz.domain.Role;
import com.sami.app.authz.dto.AssignPermissionsRequest;
import com.sami.app.authz.dto.CreateRoleRequest;
import com.sami.app.authz.dto.DuplicateRoleRequest;
import com.sami.app.authz.dto.RoleResponse;
import com.sami.app.authz.dto.UpdateRoleRequest;
import com.sami.app.authz.repository.PermissionRepository;
import com.sami.app.authz.repository.RoleRepository;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Role administration use-cases.
 *
 * <p>Guards enforced here, never in controllers: system roles cannot be renamed
 * or deleted, roles still assigned to users cannot be deleted, and the
 * super-admin role's permission set is immutable (the bypass flag makes any
 * grant list meaningless).
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<RoleResponse> list(Pageable pageable) {
        Page<Role> page = roleRepository.findAll(pageable);
        List<Long> ids = page.getContent().stream().map(Role::getId).toList();
        if (!ids.isEmpty()) {
            // Initializes the page's permission collections in one query; the
            // page entities are the same persistence-context instances.
            roleRepository.findWithPermissionsByIdIn(ids);
        }
        return page.map(role -> RoleResponse.from(role, userRepository.countByRoleId(role.getId())));
    }

    @Transactional(readOnly = true)
    public RoleResponse getById(Long id) {
        Role role = findWithPermissionsOrThrow(id);
        return RoleResponse.from(role, userRepository.countByRoleId(id));
    }

    @Transactional
    public RoleResponse create(CreateRoleRequest request) {
        requireUniqueName(request.name(), null);

        Role role = Role.builder()
                .name(request.name())
                .description(request.description())
                .build();

        return RoleResponse.from(roleRepository.save(role), 0);
    }

    @Transactional
    public RoleResponse update(Long id, UpdateRoleRequest request) {
        Role role = findWithPermissionsOrThrow(id);
        if (role.isSystem() && !role.getName().equals(request.name())) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "System roles cannot be renamed");
        }
        requireUniqueName(request.name(), id);

        role.setName(request.name());
        role.setDescription(request.description());
        // Dirty checking flushes the update on transaction commit.
        return RoleResponse.from(role, userRepository.countByRoleId(id));
    }

    @Transactional
    public void delete(Long id) {
        Role role = findOrThrow(id);
        if (role.isSystem()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "System roles cannot be deleted");
        }
        if (userRepository.countByRoleId(id) > 0) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Cannot delete a role that is still assigned to users");
        }
        roleRepository.delete(role);
    }

    /** Copies the source role's permission set; the copy is never system, super-admin or default. */
    @Transactional
    public RoleResponse duplicate(Long id, DuplicateRoleRequest request) {
        Role source = findWithPermissionsOrThrow(id);
        requireUniqueName(request.name(), null);

        Role copy = Role.builder()
                .name(request.name())
                .description(source.getDescription())
                .permissions(new HashSet<>(source.getPermissions()))
                .build();

        return RoleResponse.from(roleRepository.save(copy), 0);
    }

    /** Replaces the role's permission set in full (empty list revokes everything). */
    @Transactional
    public RoleResponse assignPermissions(Long id, AssignPermissionsRequest request) {
        Role role = findWithPermissionsOrThrow(id);
        if (role.isSuperAdmin()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "The super-admin role has unrestricted access; its permission set cannot be modified");
        }

        Set<Long> requestedIds = new HashSet<>(request.permissionIds());
        List<Permission> permissions = permissionRepository.findAllById(requestedIds);
        if (permissions.size() != requestedIds.size()) {
            throw new ResourceNotFoundException("One or more permissions were not found");
        }

        role.getPermissions().clear();
        role.getPermissions().addAll(permissions);
        return RoleResponse.from(role, userRepository.countByRoleId(id));
    }

    private void requireUniqueName(String name, Long excludedId) {
        boolean taken = excludedId == null
                ? roleRepository.existsByNameIgnoreCase(name)
                : roleRepository.existsByNameIgnoreCaseAndIdNot(name, excludedId);
        if (taken) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A role named '%s' already exists".formatted(name));
        }
    }

    private Role findOrThrow(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Role", id));
    }

    private Role findWithPermissionsOrThrow(Long id) {
        return roleRepository.findWithPermissionsById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Role", id));
    }
}
