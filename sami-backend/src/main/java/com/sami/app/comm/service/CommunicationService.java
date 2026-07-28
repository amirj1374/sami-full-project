package com.sami.app.comm.service;

import com.sami.app.comm.api.CommunicationGateway;
import com.sami.app.comm.api.SendReceipt;
import com.sami.app.comm.api.SendRequest;
import com.sami.app.comm.domain.CommChannel;
import com.sami.app.comm.domain.CommChannelType;
import com.sami.app.comm.domain.CommConversation;
import com.sami.app.comm.domain.CommMessage;
import com.sami.app.comm.domain.CommMessageAttachment;
import com.sami.app.comm.domain.CommMessageStatus;
import com.sami.app.comm.domain.CommTemplate;
import com.sami.app.comm.event.CommDomainEvent;
import com.sami.app.comm.repository.CommChannelRepository;
import com.sami.app.comm.repository.CommConversationRepository;
import com.sami.app.comm.repository.CommConversationStatusRepository;
import com.sami.app.comm.repository.CommMessageAttachmentRepository;
import com.sami.app.comm.repository.CommMessageRepository;
import com.sami.app.comm.repository.CommMessageStatusRepository;
import com.sami.app.comm.repository.CommRoutingRuleRepository;
import com.sami.app.comm.repository.CommTemplateRepository;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantDefaults;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Communication Hub's public service — the implementation of
 * {@link CommunicationGateway}.
 *
 * <p><b>Transaction shape.</b> {@link #send} is deliberately NOT transactional.
 * {@link #queue} commits the message row (the outbox), and only then is the
 * dispatcher invoked — so a provider crash cannot roll back the record, and a
 * record crash cannot have sent anything.
 *
 * <p><b>Duplicate prevention.</b> The idempotency key is checked first for a
 * friendly fast path, but the guarantee is the partial unique index on
 * {@code (tenant_id, idempotency_key)} — two concurrent sends with the same
 * key both pass the check, and exactly one insert survives; the loser returns
 * the winner's receipt.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunicationService implements CommunicationGateway {

    private final CommChannelRepository channelRepository;
    private final CommRoutingRuleRepository routingRuleRepository;
    private final CommTemplateRepository templateRepository;
    private final CommMessageRepository messageRepository;
    private final CommMessageStatusRepository messageStatusRepository;
    private final CommMessageAttachmentRepository attachmentRepository;
    private final CommConversationRepository conversationRepository;
    private final CommConversationStatusRepository conversationStatusRepository;
    private final ChannelRouter router;
    private final RecipientValidator recipientValidator;
    private final TemplateRenderer templateRenderer;
    private final RetryPolicyCalculator retryCalculator;
    private final MessageDispatcher dispatcher;
    private final CommAuditService audit;
    private final TenantDefaults tenantDefaults;
    private final ApplicationEventPublisher events;

    // -----------------------------------------------------------------
    // Public gateway
    // -----------------------------------------------------------------

    @Override
    public SendReceipt send(SendRequest request) {
        QueueOutcome outcome = queue(request, null);
        if (!outcome.duplicate() && outcome.dispatchNow()) {
            dispatcher.attempt(outcome.messageId());
        }
        return receipt(outcome);
    }

    @Override
    public SendReceipt startConversation(String subject, SendRequest firstMessage) {
        Long conversationId = createConversation(subject, firstMessage);
        QueueOutcome outcome = queue(firstMessage, conversationId);
        if (!outcome.duplicate() && outcome.dispatchNow()) {
            dispatcher.attempt(outcome.messageId());
        }
        return receipt(outcome);
    }

    @Override
    public SendReceipt reply(Long conversationId, SendRequest message) {
        requireOpenConversation(conversationId);
        QueueOutcome outcome = queue(message, conversationId);
        if (!outcome.duplicate() && outcome.dispatchNow()) {
            dispatcher.attempt(outcome.messageId());
        }
        return receipt(outcome);
    }

    // -----------------------------------------------------------------
    // Queuing — the transactional outbox write
    // -----------------------------------------------------------------

    record QueueOutcome(Long messageId, String messageNumber, String statusCode,
                       String channelCode, boolean duplicate, boolean dispatchNow) { }

    @Transactional
    protected QueueOutcome queue(SendRequest request, Long conversationId) {
        Long tenantId = tenantDefaults.current();

        // Fast-path dedup; the unique index below is the real guarantee.
        if (request.idempotencyKey() != null) {
            Optional<CommMessage> existing = messageRepository
                    .findByTenantIdAndIdempotencyKey(tenantId, request.idempotencyKey());
            if (existing.isPresent()) {
                CommMessage m = existing.get();
                return new QueueOutcome(m.getId(), m.getMessageNumber(),
                        m.getStatus().getCode(), m.getChannel().getCode(), true, false);
            }
        }

        // 1. Route.
        CommChannel explicit = null;
        if (request.channelCode() != null) {
            explicit = channelRepository.findByCode(request.channelCode())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Channel '%s' not found".formatted(request.channelCode())));
        }
        int priority = request.priority() != null ? request.priority() : 0;
        ChannelRouter.Route route = router.route(explicit,
                routingRuleRepository.findByIsActiveTrueOrderByPriorityDesc(),
                request.moduleCode(), priority, Instant.now());
        CommChannel channel = route.channel();
        CommChannelType type = channel.getChannelType();

        // 2. Validate recipient against the channel type's requirements.
        String recipient = type.isRequiresPhone()
                ? recipientValidator.normalizePhone(request.recipientAddress())
                : request.recipientAddress();
        recipientValidator.validate(type, recipient);

        // 3. Resolve content: template (fail-closed rendering) or literal body.
        String language = request.language() != null ? request.language() : "fa";
        CommTemplate template = null;
        String subject = request.subject();
        String body = request.body();
        if (request.templateCode() != null) {
            template = resolveTemplate(request.templateCode(), language, type);
            subject = templateRenderer.render(template.getSubjectTemplate(), subjectVariables(template, request));
            body = templateRenderer.render(template.getBodyTemplate(), request.templateVariables());
        }
        if (body == null || body.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "A message needs a body or a template");
        }
        if (type.getMaxBodyLength() > 0 && body.length() > type.getMaxBodyLength()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Body is %d characters; channel type %s allows %d"
                            .formatted(body.length(), type.getCode(), type.getMaxBodyLength()));
        }
        if (!type.isSupportsSubject()) {
            subject = null;
        }

        // 4. Attachment rules.
        List<SendRequest.AttachmentInput> attachments =
                request.attachments() == null ? List.of() : request.attachments();
        if (!attachments.isEmpty()) {
            if (!type.isSupportsAttachments()) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Channel type %s does not support attachments".formatted(type.getCode()));
            }
            long total = attachments.stream().mapToLong(SendRequest.AttachmentInput::sizeBytes).sum();
            if (type.getMaxAttachmentBytes() > 0 && total > type.getMaxAttachmentBytes()) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Attachments total %d bytes; channel type %s allows %d"
                                .formatted(total, type.getCode(), type.getMaxAttachmentBytes()));
            }
        }

        // 5. Persist the outbox row.
        CommMessageStatus queued = messageStatusRepository.findFirstByIsDefaultTrue()
                .orElseThrow(() -> new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "No default message status is configured"));

        Instant now = Instant.now();
        CommMessage message = CommMessage.builder()
                .messageNumber("MSG-%07d".formatted(messageRepository.nextMessageSequence()))
                .conversationId(conversationId)
                .channel(channel)
                .status(queued)
                .template(template)
                .senderUserId(CurrentActor.id())
                .recipientAddress(recipient)
                .customerId(request.customerId())
                .supplierId(request.supplierId())
                .moduleCode(request.moduleCode())
                .relatedEntityType(request.relatedEntityType())
                .relatedEntityId(request.relatedEntityId())
                .subject(subject)
                .body(body)
                .language(language)
                .priority(priority)
                .idempotencyKey(request.idempotencyKey())
                .queuedAt(now)
                // Routing deferral (business hours) parks it for the sweep.
                .nextAttemptAt(route.notBefore())
                .tenantId(tenantId)
                .build();
        channel.getDeliveryPolicy();
        retryCalculator.expiresAt(
                channel.getDeliveryPolicy() != null ? channel.getDeliveryPolicy() : defaultPolicyOf(channel),
                now).ifPresent(message::setExpiresAt);

        try {
            messageRepository.saveAndFlush(message);
        } catch (DataIntegrityViolationException e) {
            // Concurrent duplicate: the other transaction won the unique
            // index race. Absorb, return the winner.
            if (request.idempotencyKey() != null) {
                Optional<CommMessage> winner = messageRepository
                        .findByTenantIdAndIdempotencyKey(tenantId, request.idempotencyKey());
                if (winner.isPresent()) {
                    CommMessage m = winner.get();
                    return new QueueOutcome(m.getId(), m.getMessageNumber(),
                            m.getStatus().getCode(), m.getChannel().getCode(), true, false);
                }
            }
            throw e;
        }

        for (SendRequest.AttachmentInput attachment : attachments) {
            attachmentRepository.save(CommMessageAttachment.builder()
                    .messageId(message.getId())
                    .fileUuid(attachment.fileUuid())
                    .fileName(attachment.fileName())
                    .contentType(attachment.contentType())
                    .sizeBytes(attachment.sizeBytes())
                    .build());
        }

        audit.record(CommAuditService.MESSAGE, message.getId(), CommAuditService.CREATED, null,
                Map.of("messageNumber", message.getMessageNumber(),
                       "channel", channel.getCode(),
                       "recipient", recipient,
                       "deferred", route.notBefore() != null));

        events.publishEvent(CommDomainEvent.of(CommDomainEvent.MESSAGE_QUEUED,
                message.getId(), message.getMessageNumber(), conversationId,
                channel.getCode(), request.moduleCode(),
                Map.of("rule", route.ruleCode() == null ? "explicit" : route.ruleCode())));

        return new QueueOutcome(message.getId(), message.getMessageNumber(), queued.getCode(),
                channel.getCode(), false, route.notBefore() == null);
    }

    // -----------------------------------------------------------------
    // Conversations
    // -----------------------------------------------------------------

    @Transactional
    protected Long createConversation(String subject, SendRequest firstMessage) {
        CommChannel channel = firstMessage.channelCode() != null
                ? channelRepository.findByCode(firstMessage.channelCode())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Channel '%s' not found".formatted(firstMessage.channelCode())))
                : router.route(null, routingRuleRepository.findByIsActiveTrueOrderByPriorityDesc(),
                        firstMessage.moduleCode(),
                        firstMessage.priority() != null ? firstMessage.priority() : 0,
                        Instant.now()).channel();
        if (!channel.getChannelType().isSupportsConversations()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Channel type %s does not support conversations"
                            .formatted(channel.getChannelType().getCode()));
        }
        CommConversation conversation = conversationRepository.save(CommConversation.builder()
                .conversationNumber("CNV-%06d".formatted(conversationRepository.nextConversationSequence()))
                .subject(subject)
                .channel(channel)
                .status(conversationStatusRepository.findFirstByIsDefaultTrue()
                        .orElseThrow(() -> new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                                "No default conversation status is configured")))
                .ownerUserId(CurrentActor.id())
                .customerId(firstMessage.customerId())
                .supplierId(firstMessage.supplierId())
                .moduleCode(firstMessage.moduleCode())
                .relatedEntityType(firstMessage.relatedEntityType())
                .relatedEntityId(firstMessage.relatedEntityId())
                .tenantId(tenantDefaults.current())
                .build());

        audit.record(CommAuditService.CONVERSATION, conversation.getId(), CommAuditService.CREATED,
                null, Map.of("conversationNumber", conversation.getConversationNumber()));
        events.publishEvent(CommDomainEvent.of(CommDomainEvent.CONVERSATION_STARTED,
                null, null, conversation.getId(), channel.getCode(),
                firstMessage.moduleCode(), Map.of()));
        return conversation.getId();
    }

    @Transactional
    public CommConversation closeConversation(Long conversationId) {
        CommConversation conversation = conversationRepository.findWithDetailsById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conversation %d not found".formatted(conversationId)));
        conversationStatusRepository.findFirstByIsTerminalTrue()
                .ifPresent(conversation::setStatus);
        conversation.setClosedAt(Instant.now());
        audit.record(CommAuditService.CONVERSATION, conversationId, CommAuditService.CLOSED,
                null, Map.of());
        events.publishEvent(CommDomainEvent.of(CommDomainEvent.CONVERSATION_CLOSED,
                null, null, conversationId, conversation.getChannel().getCode(),
                conversation.getModuleCode(), Map.of()));
        return conversation;
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private void requireOpenConversation(Long conversationId) {
        CommConversation conversation = conversationRepository.findWithDetailsById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conversation %d not found".formatted(conversationId)));
        if (!conversation.getStatus().isAllowsReply()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Conversation %s is %s and does not accept replies"
                            .formatted(conversation.getConversationNumber(),
                                    conversation.getStatus().getName().toLowerCase()));
        }
        conversation.setLastActivityAt(Instant.now());
    }

    /**
     * Template lookup with a language fallback: exact language first, then any
     * active revision of the code — a missing Persian translation must not
     * block an English OTP.
     */
    private CommTemplate resolveTemplate(String code, String language, CommChannelType type) {
        CommTemplate template = templateRepository
                .findByCodeAndLanguageAndIsActiveTrue(code, language)
                .or(() -> templateRepository.findByCodeOrderByRevisionDesc(code).stream()
                        .filter(CommTemplate::isActive)
                        .findFirst())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active template '%s'".formatted(code)));
        if (template.getChannelType() != null
                && !template.getChannelType().getId().equals(type.getId())) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Template '%s' is bound to channel type %s, not %s".formatted(
                            code, template.getChannelType().getCode(), type.getCode()));
        }
        return template;
    }

    /**
     * Subject templates share the request's variable map but almost never use
     * every variable, so surplus values are legitimate there — only the BODY
     * enforces exact-match. The subject still fails closed on MISSING values.
     */
    private Map<String, Object> subjectVariables(CommTemplate template, SendRequest request) {
        if (template.getSubjectTemplate() == null || request.templateVariables() == null) {
            return Map.of();
        }
        var referenced = templateRenderer.referencedVariables(template.getSubjectTemplate());
        return request.templateVariables().entrySet().stream()
                .filter(e -> referenced.contains(e.getKey()))
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private com.sami.app.comm.domain.CommDeliveryPolicy defaultPolicyOf(CommChannel channel) {
        // Expiry is policy-driven; a channel with no policy simply never expires
        // messages, which is the conservative default for an outbox.
        return com.sami.app.comm.domain.CommDeliveryPolicy.builder()
                .maxAttempts(3).retryBaseSeconds(60)
                .retryBackoffMultiplier(java.math.BigDecimal.valueOf(2.0))
                .retryMaxSeconds(3600).timeoutSeconds(30).expirationMinutes(0)
                .build();
    }

    private SendReceipt receipt(QueueOutcome outcome) {
        // Reload: the dispatcher may have advanced the status since queuing.
        return messageRepository.findWithDetailsById(outcome.messageId())
                .map(m -> new SendReceipt(m.getId(), m.getMessageNumber(),
                        m.getStatus().getCode(), m.getChannel().getCode(), outcome.duplicate()))
                .orElse(new SendReceipt(outcome.messageId(), outcome.messageNumber(),
                        outcome.statusCode(), outcome.channelCode(), outcome.duplicate()));
    }
}
