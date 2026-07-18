package com.sami.app.authz.service;

import com.sami.app.authz.domain.AppModule;
import com.sami.app.authz.domain.Permission;
import com.sami.app.authz.dto.CreateModuleRequest;
import com.sami.app.authz.dto.ModuleResponse;
import com.sami.app.authz.dto.UpdateModuleRequest;
import com.sami.app.authz.dto.UpdateModuleStatusRequest;
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

/**
 * Module administration use-cases.
 *
 * <p>Modules are the self-service unit of extension: creating one (optionally
 * with the six standard permissions) instantly gives it a menu entry and a
 * grantable permission namespace. System modules host the admin UI itself and
 * can be neither disabled nor deleted.
 */
@Service
@RequiredArgsConstructor
public class ModuleService {

    /** The six standard actions created for a new module on request. */
    private static final List<String> STANDARD_ACTIONS =
            List.of("view", "create", "edit", "delete", "export", "import");

    private final AppModuleRepository moduleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public Page<ModuleResponse> list(Pageable pageable) {
        return moduleRepository.findAll(pageable)
                .map(module -> ModuleResponse.from(module,
                        permissionRepository.countByModuleId(module.getId())));
    }

    @Transactional
    public ModuleResponse create(CreateModuleRequest request) {
        if (moduleRepository.existsByCodeIgnoreCase(request.code())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A module with code '%s' already exists".formatted(request.code()));
        }

        AppModule module = AppModule.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .icon(request.icon())
                .path(request.path())
                .displayOrder(request.displayOrder())
                .enabled(request.enabled())
                .build();
        moduleRepository.save(module);

        long permissionCount = 0;
        if (request.createDefaultPermissions()) {
            permissionCount = createStandardPermissions(module);
        }
        return ModuleResponse.from(module, permissionCount);
    }

    @Transactional
    public ModuleResponse update(Long id, UpdateModuleRequest request) {
        AppModule module = findOrThrow(id);
        module.setName(request.name());
        module.setDescription(request.description());
        module.setIcon(request.icon());
        module.setPath(request.path());
        module.setDisplayOrder(request.displayOrder());
        // Dirty checking flushes the update on transaction commit.
        return ModuleResponse.from(module, permissionRepository.countByModuleId(id));
    }

    @Transactional
    public ModuleResponse updateStatus(Long id, UpdateModuleStatusRequest request) {
        AppModule module = findOrThrow(id);
        if (module.isSystem() && !request.enabled()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "System modules cannot be disabled");
        }
        module.setEnabled(request.enabled());
        return ModuleResponse.from(module, permissionRepository.countByModuleId(id));
    }

    @Transactional
    public void delete(Long id) {
        AppModule module = findOrThrow(id);
        if (module.isSystem()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "System modules cannot be deleted");
        }
        // Database FKs cascade the module's permissions and any role grants.
        moduleRepository.delete(module);
    }

    /**
     * Creates the six standard actions for a new module, mirroring the seed's
     * naming ({@code "<Module name> — <Action>"}) but as non-system rows so
     * runtime-created modules stay fully editable.
     */
    private int createStandardPermissions(AppModule module) {
        List<Permission> permissions = STANDARD_ACTIONS.stream()
                .map(action -> Permission.builder()
                        .module(module)
                        .action(action)
                        .code(module.getCode() + ":" + action)
                        .name(module.getName() + " — " + capitalize(action))
                        .build())
                .toList();
        permissionRepository.saveAll(permissions);
        return permissions.size();
    }

    private static String capitalize(String action) {
        return Character.toUpperCase(action.charAt(0)) + action.substring(1);
    }

    private AppModule findOrThrow(Long id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Module", id));
    }
}
