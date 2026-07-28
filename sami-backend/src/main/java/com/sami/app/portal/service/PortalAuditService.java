package com.sami.app.portal.service;

import com.sami.app.common.tenancy.TenantDefaults;
import com.sami.app.portal.domain.PortalAuditLog;
import com.sami.app.portal.repository.PortalAuditLogRepository;
import com.sami.app.portal.security.PortalActor;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * Portal audit trail. Records IP, user agent and device on every action — the
 * spec requires it, and for an internet-facing surface it is the only way to
 * investigate a suspicious login after the fact.
 */
@Service
@RequiredArgsConstructor
public class PortalAuditService {

    public static final String REGISTERED = "Registered";
    public static final String VERIFIED = "Verified";
    public static final String LOGIN = "Login";
    public static final String LOGIN_FAILED = "LoginFailed";
    public static final String LOGOUT = "Logout";
    public static final String LOCKED = "Locked";
    public static final String PROFILE_UPDATED = "ProfileUpdated";
    public static final String DOCUMENT_DOWNLOADED = "DocumentDownloaded";
    public static final String DOCUMENT_UPLOADED = "DocumentUploaded";
    public static final String REQUEST_SUBMITTED = "RequestSubmitted";
    public static final String SESSION_REVOKED = "SessionRevoked";
    public static final String CAPABILITIES_CHANGED = "CapabilitiesChanged";

    private final PortalAuditLogRepository repository;
    private final TenantDefaults tenantDefaults;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long accountId, String entityType, Long entityId, String action,
                       Map<String, Object> oldValues, Map<String, Object> newValues) {
        boolean staff = PortalActor.get().isEmpty() && CurrentActor.id() != null;
        repository.save(PortalAuditLog.builder()
                .accountId(accountId)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValues(oldValues == null || oldValues.isEmpty() ? null : oldValues)
                .newValues(newValues == null || newValues.isEmpty() ? null : newValues)
                .actorKind(staff ? "STAFF" : PortalActor.get().isPresent() ? "CUSTOMER" : "SYSTEM")
                .actorId(staff ? CurrentActor.id() : PortalActor.accountId())
                .actorLabel(staff ? CurrentActor.email() : null)
                .ipAddress(header(null))
                .deviceLabel(header("X-Device-Label"))
                .userAgent(header("User-Agent"))
                
                // tenant_id is mapped, so Hibernate always sends it: an explicit
                // NULL would override the column DEFAULT instead of triggering it.
                .tenantId(tenantDefaults.current())
                .build());
    }

    private String header(String name) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        if (name == null) {
            String forwarded = attrs.getRequest().getHeader("X-Forwarded-For");
            return forwarded != null && !forwarded.isBlank()
                    ? forwarded.split(",")[0].trim()
                    : attrs.getRequest().getRemoteAddr();
        }
        String value = attrs.getRequest().getHeader(name);
        return value == null ? null : value.substring(0, Math.min(value.length(), 500));
    }
}
