package com.sami.app.portal.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** The module's published business event. */
public record PortalDomainEvent(String eventId, String eventType, Long accountId,
                                Long customerId, Map<String, Object> payload, Instant occurredAt) {

    public static final String CUSTOMER_PORTAL_REGISTERED = "CustomerPortalRegistered";
    public static final String CUSTOMER_LOGGED_IN = "CustomerLoggedIn";
    public static final String PROFILE_UPDATED = "ProfileUpdated";
    public static final String REQUEST_SUBMITTED = "RequestSubmitted";
    public static final String REQUEST_RESOLVED = "RequestResolved";
    public static final String DOCUMENT_DOWNLOADED = "DocumentDownloaded";
    public static final String DOCUMENT_UPLOADED = "DocumentUploaded";
    public static final String ACCOUNT_LOCKED = "AccountLocked";
    public static final String MESSAGE_SENT = "MessageSent";

    public static PortalDomainEvent of(String eventType, Long accountId, Long customerId,
                                       Map<String, Object> payload) {
        return new PortalDomainEvent(UUID.randomUUID().toString(), eventType, accountId,
                customerId, payload == null ? Map.of() : payload, Instant.now());
    }
}
