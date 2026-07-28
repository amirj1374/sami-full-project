package com.sami.app.comm.spi;

import java.util.List;
import java.util.Map;

/**
 * What a provider handler is asked to deliver — a flattened, provider-neutral
 * view of one message. Handlers never see JPA entities.
 *
 * @param providerConfig the provider row's {@code config} JSONB, already
 *                       parsed. Contains endpoints and env-var NAMES; the
 *                       handler resolves secrets from the environment itself —
 *                       secrets are never stored in the database.
 */
public record OutboundMessage(
        Long messageId,
        String messageNumber,
        String channelTypeCode,
        String senderAddress,
        String recipientAddress,
        String subject,
        String body,
        String language,
        List<AttachmentRef> attachments,
        Map<String, Object> providerConfig,
        int timeoutSeconds
) {
    /** Reference into the files module; the handler downloads if it needs bytes. */
    public record AttachmentRef(String fileUuid, String fileName, String contentType, long sizeBytes) { }
}
