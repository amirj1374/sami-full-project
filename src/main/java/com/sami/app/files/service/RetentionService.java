package com.sami.app.files.service;

import com.sami.app.files.domain.FileStatus;
import com.sami.app.files.domain.ManagedFile;
import com.sami.app.files.domain.RetentionPolicy;
import com.sami.app.files.event.FileDomainEvent;
import com.sami.app.files.repository.FileStatusRepository;
import com.sami.app.files.repository.FileUploadSessionRepository;
import com.sami.app.files.repository.ManagedFileRepository;
import com.sami.app.files.spi.RetentionHandler;
import com.sami.app.files.spi.RetentionHandlerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Applies expired retention policies.
 *
 * <p>Invoked explicitly today; it becomes a scheduled job once the shared
 * scheduler exists (docs/platform-architecture.md D-D). No {@code @Scheduled}
 * here, because a per-module cron is exactly the duplicated infrastructure the
 * architecture rules forbid.
 *
 * <p>Legal hold always wins: held files are filtered out by the query, so a
 * handler is never even offered one.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetentionService {

    private final ManagedFileRepository fileRepository;
    private final FileStatusRepository statusRepository;
    private final FileUploadSessionRepository uploadSessionRepository;
    private final RetentionHandlerRegistry handlerRegistry;
    private final FileAuditService auditService;
    private final ApplicationEventPublisher events;

    @Transactional
    public Summary applyExpired() {
        Instant now = Instant.now();
        List<ManagedFile> expired = fileRepository.findExpired(now);

        int archived = 0;
        int deleted = 0;
        int notified = 0;
        int skipped = 0;

        for (ManagedFile file : expired) {
            RetentionPolicy policy = file.getRetentionPolicy();
            if (policy == null) {
                skipped++;
                continue;
            }
            Optional<RetentionHandler> handler = handlerRegistry.find(policy.getActionOnExpiry());
            if (handler.isEmpty()) {
                log.warn("No retention handler for action '{}' (policy {})",
                        policy.getActionOnExpiry(), policy.getCode());
                skipped++;
                continue;
            }

            String outcome = handler.get().apply(file.getId());

            // The state change happens here, never inside the handler, so a
            // handler can never partially destroy data.
            switch (policy.getActionOnExpiry()) {
                case "delete" -> {
                    applyStatus(file, statusRepository.findFirstByIsDeletedStateTrue());
                    file.setDeletedAt(now);
                    deleted++;
                }
                case "archive" -> {
                    applyStatus(file, statusRepository.findFirstByIsArchivedStateTrue());
                    archived++;
                }
                case "anonymise" -> {
                    file.setDescription(null);
                    file.setOwnerEmail(null);
                    file.setUploadedIp(null);
                    archived++;
                }
                default -> notified++;
            }

            fileRepository.save(file);
            auditService.record("file", file.getId(), FileAuditService.ACTION_RETENTION_APPLIED,
                    Map.of("policy", policy.getCode()), Map.of("outcome", outcome));
            events.publishEvent(FileDomainEvent.of(FileDomainEvent.RETENTION_EXPIRED,
                    file.getFileUuid(), file.getId(), file.getModuleCode(),
                    file.getEntityCode(), file.getEntityId(),
                    Map.of("policy", policy.getCode(), "action", policy.getActionOnExpiry())));
        }

        int reclaimed = reclaimStaleUploads(now);
        return new Summary(expired.size(), archived, deleted, notified, skipped, reclaimed);
    }

    /**
     * Closes upload sessions that were never completed — the interrupted-upload
     * edge case. Without this, a client that dies mid-transfer leaves a session
     * open forever.
     */
    @Transactional
    public int reclaimStaleUploads(Instant now) {
        var stale = uploadSessionRepository.findAllByStatusAndExpiresAtBefore("open", now);
        stale.forEach(session -> {
            session.setStatus("expired");
            session.setFailureReason("Upload session expired before completion");
            uploadSessionRepository.save(session);
        });
        return stale.size();
    }

    private void applyStatus(ManagedFile file, Optional<FileStatus> status) {
        status.ifPresent(file::setStatus);
    }

    public record Summary(int examined, int archived, int deleted,
                          int notified, int skipped, int reclaimedUploads) {
    }
}
