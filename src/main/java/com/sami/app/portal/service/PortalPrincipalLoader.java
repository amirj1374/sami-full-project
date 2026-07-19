package com.sami.app.portal.service;

import com.sami.app.portal.domain.PortalAccount;
import com.sami.app.portal.domain.PortalSession;
import com.sami.app.portal.repository.PortalAccountCapabilityRepository;
import com.sami.app.portal.repository.PortalAccountRepository;
import com.sami.app.portal.repository.PortalSessionRepository;
import com.sami.app.portal.security.PortalPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;

/**
 * Rebuilds the authenticated principal from the database on every request.
 *
 * <p>Capabilities are deliberately not carried in the token, matching the staff
 * realm's rule. The consequence is that locking an account, suspending it or
 * revoking a session takes effect on the very next request rather than at token
 * expiry — which is exactly the "account locked while an active session exists"
 * edge case.
 */
@Service
@RequiredArgsConstructor
public class PortalPrincipalLoader {

    private final PortalAccountRepository accountRepository;
    private final PortalSessionRepository sessionRepository;
    private final PortalAccountCapabilityRepository capabilityRepository;

    @Transactional
    public Optional<PortalPrincipal> load(Long accountId, Long sessionId) {
        PortalAccount account = accountRepository.findById(accountId).orElse(null);
        if (account == null || !account.canLogIn()) {
            return Optional.empty();
        }

        // A revoked or expired session invalidates an otherwise-valid token.
        if (sessionId != null) {
            PortalSession session = sessionRepository.findById(sessionId).orElse(null);
            if (session == null || !session.isActive()
                    || !session.getAccountId().equals(accountId)) {
                return Optional.empty();
            }
            session.setLastSeenAt(Instant.now());
            sessionRepository.save(session);
        }

        return Optional.of(new PortalPrincipal(
                account.getId(),
                account.getCustomerId(),
                account.getTenantId(),
                account.getUsername(),
                account.getPreferredLanguage(),
                account.getPreferredTimezone(),
                new HashSet<>(capabilityRepository.findCapabilityCodes(accountId)),
                sessionId));
    }
}
