package com.sami.app.user.service;

import com.sami.app.security.SecurityUser;
import com.sami.app.user.domain.User;
import com.sami.app.user.domain.UserAuditLog;
import com.sami.app.user.dto.UserAuditLogResponse;
import com.sami.app.user.repository.UserAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Records and serves the append-only user audit trail.
 *
 * <p>Entries are written inside the calling transaction (an action and its audit
 * row commit or roll back together). The actor is resolved from the security
 * context; system-initiated actions (e.g. bootstrap) record no actor.
 */
@Service
@RequiredArgsConstructor
public class UserAuditService {

    /** Audit action names. Constants, not an enum: modules may log custom actions. */
    public static final String CREATED = "CREATED";
    public static final String UPDATED = "UPDATED";
    public static final String STATUS_CHANGED = "STATUS_CHANGED";
    public static final String ARCHIVED = "ARCHIVED";
    public static final String RESTORED = "RESTORED";
    public static final String DELETED = "DELETED";
    public static final String PURGED = "PURGED";
    public static final String AVATAR_UPDATED = "AVATAR_UPDATED";

    private final UserAuditLogRepository auditLogRepository;

    /** Appends one entry. {@code oldValues}/{@code newValues} hold only changed fields. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(User user, String action,
                       Map<String, Object> oldValues, Map<String, Object> newValues) {
        SecurityUser actor = currentActor();
        auditLogRepository.save(UserAuditLog.builder()
                .userId(user.getId())
                .userEmail(user.getEmail())
                .action(action)
                .actorId(actor != null ? actor.getId() : null)
                .actorEmail(actor != null ? actor.getUsername() : null)
                .oldValues(oldValues == null || oldValues.isEmpty() ? null : oldValues)
                .newValues(newValues == null || newValues.isEmpty() ? null : newValues)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<UserAuditLogResponse> listForUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(UserAuditLogResponse::from);
    }

    private SecurityUser currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser principal) {
            return principal;
        }
        return null;
    }
}
