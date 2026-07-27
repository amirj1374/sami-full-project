package com.sami.app.comm.service;

import com.sami.app.comm.domain.CommChannel;
import com.sami.app.comm.domain.CommDeliveryPolicy;
import com.sami.app.comm.domain.CommMessage;
import com.sami.app.comm.repository.CommDeliveryPolicyRepository;
import com.sami.app.comm.repository.CommMessageAttachmentRepository;
import com.sami.app.comm.repository.CommMessageRepository;
import com.sami.app.comm.spi.CommProviderHandler;
import com.sami.app.comm.spi.CommProviderRegistry;
import com.sami.app.comm.spi.DeliveryResult;
import com.sami.app.comm.spi.OutboundMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Executes one delivery attempt.
 *
 * <p>Deliberately NOT transactional: the provider call is slow I/O and must
 * hold no database connection. Callers (the send path and the sweep) invoke
 * {@link #attempt} AFTER their own transaction has committed the message row,
 * so a crash mid-delivery can never lose the message — only leave it queued.
 * All state transitions go through {@link MessageStateRecorder}, a separate
 * bean, so each runs in a genuine transaction (the V19 proxy lesson).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageDispatcher {

    private final CommMessageRepository messageRepository;
    private final CommMessageAttachmentRepository attachmentRepository;
    private final CommDeliveryPolicyRepository policyRepository;
    private final CommProviderRegistry providerRegistry;
    private final RetryPolicyCalculator retryCalculator;
    private final MessageStateRecorder recorder;

    /** Attempts delivery of a committed message. Never throws for delivery problems. */
    public void attempt(Long messageId) {
        CommMessage message = messageRepository.findWithDetailsById(messageId).orElse(null);
        if (message == null) {
            log.warn("Dispatch requested for missing message {}", messageId);
            return;
        }
        if (message.getStatus().isTerminal()) {
            return; // Cancelled or expired while queued — nothing to do.
        }
        // Deferred by routing (business hours): not due yet, leave for the sweep.
        Instant now = Instant.now();
        if (message.getStatus().isQueuedState()
                && message.getNextAttemptAt() != null && message.getNextAttemptAt().isAfter(now)) {
            return;
        }

        CommDeliveryPolicy policy = policyOf(message.getChannel());

        // Checked before EVERY attempt: an OTP queued five minutes ago must
        // not arrive late — that is worse than not arriving.
        if (retryCalculator.isExpired(message.getExpiresAt(), now)) {
            recorder.markExpired(messageId);
            return;
        }
        if (!message.getChannel().canSend()) {
            // Channel went down after queuing: push back without burning an
            // attempt, so a maintenance window doesn't exhaust the budget.
            recorder.reschedule(messageId, policy, "Channel cannot send (status or provider inactive)");
            return;
        }

        recorder.markSending(messageId);
        DeliveryResult result = deliver(message, policy);
        recorder.recordOutcome(messageId, policy, result);
    }

    /** The provider call — no transaction is open here. */
    private DeliveryResult deliver(CommMessage message, CommDeliveryPolicy policy) {
        CommChannel channel = message.getChannel();
        Optional<CommProviderHandler> handler =
                providerRegistry.find(channel.getProvider().getHandlerKey());
        if (handler.isEmpty()) {
            // Inert-provider contract: the row exists, the bean does not yet.
            // Retryable — the bean may arrive with the next deployment.
            return DeliveryResult.retryableFailure(
                    "No handler registered for provider key '%s'"
                            .formatted(channel.getProvider().getHandlerKey()));
        }
        try {
            var attachments = attachmentRepository.findByMessageId(message.getId()).stream()
                    .map(a -> new OutboundMessage.AttachmentRef(
                            a.getFileUuid().toString(), a.getFileName(),
                            a.getContentType(), a.getSizeBytes()))
                    .toList();
            return handler.get().deliver(new OutboundMessage(
                    message.getId(), message.getMessageNumber(),
                    channel.getChannelType().getCode(),
                    channel.getSenderAddress(), message.getRecipientAddress(),
                    message.getSubject(), message.getBody(), message.getLanguage(),
                    attachments,
                    channel.getProvider().getConfig() == null ? Map.of() : channel.getProvider().getConfig(),
                    policy.getTimeoutSeconds()));
        } catch (RuntimeException e) {
            // Handlers must not throw; one that does is an infrastructure
            // fault, treated as retryable.
            log.error("Provider handler '{}' threw for message {}",
                    channel.getProvider().getHandlerKey(), message.getMessageNumber(), e);
            return DeliveryResult.retryableFailure("Handler error: " + e.getMessage());
        }
    }

    private CommDeliveryPolicy policyOf(CommChannel channel) {
        if (channel.getDeliveryPolicy() != null) {
            return channel.getDeliveryPolicy();
        }
        return policyRepository.findFirstByIsDefaultTrue()
                .orElseThrow(() -> new IllegalStateException("No default delivery policy is configured"));
    }
}
