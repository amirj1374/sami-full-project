package com.sami.app.comm.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The module's public send contract — what a business module (or the future
 * notification centre) hands over. Everything after this is the hub's problem.
 *
 * @param templateCode   optional; body/subject are rendered from it when set,
 *                       and {@code templateVariables} must then match its
 *                       declared variables exactly (fail-closed both ways)
 * @param channelCode    optional explicit channel; NULL routes by rule
 * @param idempotencyKey optional caller dedup key — two sends with the same
 *                       key yield one message, enforced by the database
 */
public record SendRequest(
        String recipientAddress,
        Long customerId,
        Long supplierId,
        String moduleCode,
        String relatedEntityType,
        Long relatedEntityId,
        String channelCode,
        String templateCode,
        Map<String, Object> templateVariables,
        String subject,
        String body,
        String language,
        Integer priority,
        String idempotencyKey,
        Long conversationId,
        List<AttachmentInput> attachments
) {
    public record AttachmentInput(UUID fileUuid, String fileName, String contentType, long sizeBytes) { }
}
