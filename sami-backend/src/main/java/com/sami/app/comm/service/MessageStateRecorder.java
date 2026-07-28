package com.sami.app.comm.service;

import com.sami.app.comm.domain.CommDeliveryPolicy;
import com.sami.app.comm.domain.CommMessage;
import com.sami.app.comm.event.CommDomainEvent;
import com.sami.app.comm.repository.CommMessageRepository;
import com.sami.app.comm.repository.CommMessageStatusRepository;
import com.sami.app.comm.spi.DeliveryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Message state transitions, each in its own short transaction.
 *
 * <p>A separate bean from {@code MessageDispatcher} for the V19 reason:
 * {@code @Transactional} is proxy-based, and the dispatcher calling annotated
 * methods on ITSELF would silently run them with no transaction at all. Going
 * through this bean's proxy makes each transition genuinely atomic while the
 * slow provider I/O between them holds no database connection.
 */
@Service
@RequiredArgsConstructor
public class MessageStateRecorder {

    private final CommMessageRepository messageRepository;
    private final CommMessageStatusRepository statusRepository;
    private final RetryPolicyCalculator retryCalculator;
    private final ApplicationEventPublisher events;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSending(Long messageId) {
        messageRepository.findById(messageId).ifPresent(m -> {
            statusRepository.findFirstByIsSendingStateTrue().ifPresent(m::setStatus);
            m.setAttempts(m.getAttempts() + 1);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOutcome(Long messageId, CommDeliveryPolicy policy, DeliveryResult result) {
        CommMessage message = messageRepository.findWithDetailsById(messageId).orElse(null);
        if (message == null) {
            return;
        }
        Instant now = Instant.now();

        if (result.success()) {
            statusRepository.findFirstByIsSentStateTrue().ifPresent(message::setStatus);
            message.setSentAt(now);
            message.setNextAttemptAt(null);
            message.setLastError(null);
            message.setProviderMessageRef(result.providerMessageRef());
            publish(CommDomainEvent.MESSAGE_SENT, message, Map.of("attempts", message.getAttempts()));
            return;
        }

        message.setLastError(truncate(result.error()));
        message.setFailedAt(now);

        Optional<Instant> next = result.retryable()
                ? retryCalculator.nextAttemptAt(policy, message.getAttempts(), now)
                : Optional.empty();

        if (next.isPresent()) {
            // Failed but retryable: the sweep picks it up at nextAttemptAt.
            statusRepository.findFirstByIsFailedStateTrueAndAllowsRetryTrue()
                    .ifPresent(message::setStatus);
            message.setNextAttemptAt(next.get());
        } else {
            // Permanent failure, or retries exhausted: terminal (DEAD).
            statusRepository.findFirstByIsFailedStateTrueAndIsTerminalTrue()
                    .ifPresent(message::setStatus);
            message.setNextAttemptAt(null);
            publish(CommDomainEvent.MESSAGE_FAILED, message,
                    Map.of("error", String.valueOf(result.error()),
                           "attempts", message.getAttempts()));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markExpired(Long messageId) {
        messageRepository.findWithDetailsById(messageId).ifPresent(m -> {
            statusRepository.findFirstByIsExpiredStateTrue().ifPresent(m::setStatus);
            m.setNextAttemptAt(null);
            publish(CommDomainEvent.MESSAGE_EXPIRED, m, Map.of());
        });
    }

    /**
     * Pushes the next attempt back without burning an attempt — used when the
     * CHANNEL is the problem (maintenance, disabled provider), not the
     * provider's verdict on this message.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reschedule(Long messageId, CommDeliveryPolicy policy, String reason) {
        messageRepository.findById(messageId).ifPresent(m -> {
            m.setLastError(truncate(reason));
            retryCalculator.nextAttemptAt(policy, Math.max(1, m.getAttempts()), Instant.now())
                    .ifPresentOrElse(m::setNextAttemptAt, () -> {
                        statusRepository.findFirstByIsFailedStateTrueAndIsTerminalTrue()
                                .ifPresent(m::setStatus);
                        m.setNextAttemptAt(null);
                    });
        });
    }

    private void publish(String type, CommMessage message, Map<String, Object> payload) {
        events.publishEvent(CommDomainEvent.of(type, message.getId(), message.getMessageNumber(),
                message.getConversationId(), message.getChannel().getCode(),
                message.getModuleCode(), payload));
    }

    private static String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= 1000 ? error : error.substring(0, 997) + "...";
    }
}
