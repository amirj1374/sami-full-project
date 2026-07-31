package com.sami.app.licensing.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.licensing.domain.LicensingStatus;
import com.sami.app.licensing.domain.Tenant;
import com.sami.app.licensing.event.LicenseDomainEvent;
import com.sami.app.licensing.repository.LicensingStatusRepository;
import com.sami.app.licensing.repository.TenantRepository;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.sami.app.licensing.dto.LicensingDtos.TenantUpdateRequest;

/**
 * Tenant lifecycle. An on-premise installation runs exactly one tenant; a cloud
 * deployment runs many — the same code and schema serve both.
 */
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final LicensingStatusRepository statusRepository;
    private final EntitlementService entitlements;
    private final LicenseAuditService audit;
    private final ApplicationEventPublisher eventPublisher;
    private final LicensingScope scope;

    @Transactional(readOnly = true)
    public List<Tenant> list() {
        scope.requirePlatform();
        return tenantRepository.findAllBy();
    }

    @Transactional(readOnly = true)
    public Tenant get(Long id) {
        scope.requireAccessTo(id);
        return tenantRepository.findWithStatusById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + id));
    }

    @Transactional
    public Tenant create(String code, String name, String description, String contactEmail,
                         Map<String, Object> config) {
        scope.requirePlatform();
        if (tenantRepository.existsByCode(code)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "Tenant code already exists: " + code);
        }
        Tenant tenant = Tenant.builder()
                .code(code)
                .name(name)
                .description(description)
                .contactEmail(contactEmail)
                .config(config == null ? new HashMap<>() : new HashMap<>(config))
                .status(status("draft"))
                .createdBy(CurrentActor.id())
                .createdByEmail(CurrentActor.email())
                .build();
        Tenant saved = tenantRepository.save(tenant);
        audit.recordForTenant(saved.getId(), "TENANT", saved.getId(), "CREATED", null,
                Map.of("code", code, "name", name));
        publish(LicenseDomainEvent.TENANT_CREATED, saved, Map.of("code", code));
        entitlements.invalidate();
        return saved;
    }

    @Transactional
    public Tenant activate(Long id) {
        scope.requirePlatform();
        Tenant tenant = get(id);
        String from = tenant.getStatus().getCode();
        tenant.setStatus(status("active"));
        tenant.setActivatedAt(Instant.now());
        tenant.setSuspendedAt(null);
        Tenant saved = tenantRepository.save(tenant);
        audit.recordForTenant(id, "TENANT", id, "ACTIVATED",
                Map.of("status", from), Map.of("status", "active"));
        entitlements.invalidate();
        return saved;
    }

    @Transactional
    public Tenant suspend(Long id) {
        scope.requirePlatform();
        Tenant tenant = get(id);
        String from = tenant.getStatus().getCode();
        tenant.setStatus(status("suspended"));
        tenant.setSuspendedAt(Instant.now());
        Tenant saved = tenantRepository.save(tenant);
        audit.recordForTenant(id, "TENANT", id, "SUSPENDED",
                Map.of("status", from), Map.of("status", "suspended"));
        publish(LicenseDomainEvent.TENANT_SUSPENDED, saved, Map.of());
        entitlements.invalidate();
        return saved;
    }

    @Transactional
    public Tenant update(Long id, TenantUpdateRequest request) {
        scope.requirePlatform();
        Tenant tenant = get(id);
        if (request.expectedVersion() != null && request.expectedVersion() != tenant.getVersion()) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "Tenant was modified by another user; reload and retry");
        }
        Map<String, Object> before = Map.of("name", tenant.getName(), "status", tenant.getStatus().getCode());
        tenant.setName(request.name());
        tenant.setDescription(request.description());
        tenant.setContactEmail(request.contactEmail());
        tenant.setConfig(request.config() == null ? new HashMap<>() : new HashMap<>(request.config()));
        Tenant saved = tenantRepository.save(tenant);
        audit.recordForTenant(id, "TENANT", id, "UPDATED", before,
                Map.of("name", saved.getName(), "status", saved.getStatus().getCode()));
        entitlements.invalidate();
        return saved;
    }

    private LicensingStatus status(String code) {
        return statusRepository.findByScopeAndCode(LicensingStatus.SCOPE_TENANT, code)
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "Unknown tenant status: " + code));
    }

    private void publish(String type, Tenant tenant, Map<String, Object> payload) {
        eventPublisher.publishEvent(new LicenseDomainEvent(
                "tenant-" + tenant.getId(), type, tenant.getId(), null, null, payload, Instant.now()));
    }
}
