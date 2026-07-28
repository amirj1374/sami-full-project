package com.sami.app.files.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * The module's published business event. Consumers subscribe with a plain Spring
 * {@code @EventListener}; automation reaches it through {@code DomainEventBridge}.
 *
 * <p>Carries {@code fileUuid} rather than the database id because that is the
 * only identifier business modules are allowed to hold.
 */
public record FileDomainEvent(
        String eventId,
        String eventType,
        UUID fileUuid,
        Long fileId,
        String moduleCode,
        String entityCode,
        Long entityId,
        Map<String, Object> payload,
        Instant occurredAt
) {

    public static final String FILE_UPLOADED = "FileUploaded";
    public static final String FILE_UPDATED = "FileUpdated";
    public static final String FILE_DOWNLOADED = "FileDownloaded";
    public static final String FILE_DELETED = "FileDeleted";
    public static final String FILE_RESTORED = "FileRestored";
    public static final String VERSION_CREATED = "VersionCreated";
    public static final String VERSION_ROLLED_BACK = "VersionRolledBack";
    public static final String PREVIEW_GENERATED = "PreviewGenerated";
    public static final String THUMBNAIL_GENERATED = "ThumbnailGenerated";
    public static final String STORAGE_CHANGED = "StorageChanged";
    public static final String RETENTION_EXPIRED = "RetentionExpired";
    public static final String FILE_QUARANTINED = "FileQuarantined";
    public static final String VIRUS_DETECTED = "VirusDetected";
    public static final String QUOTA_EXCEEDED = "QuotaExceeded";

    public static FileDomainEvent of(String eventType, UUID fileUuid, Long fileId,
                                     String moduleCode, String entityCode, Long entityId,
                                     Map<String, Object> payload) {
        return new FileDomainEvent(UUID.randomUUID().toString(), eventType, fileUuid, fileId,
                moduleCode, entityCode, entityId,
                payload == null ? Map.of() : payload, Instant.now());
    }
}
