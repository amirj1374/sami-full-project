package com.sami.app.comm.api;

/**
 * The Communication Hub's public service — the ONLY door business modules may
 * use to reach a person over any channel. Nothing outside the comm package
 * touches providers, channels or delivery mechanics.
 *
 * <p>Published as an interface for the same reason as WorkingTimeProvider: the
 * seam is the contract, and callers stay ignorant of the machinery.
 */
public interface CommunicationGateway {

    /**
     * Queues a message and attempts immediate dispatch.
     *
     * <p>The message row is committed even when dispatch fails — the outbound
     * sweep retries it under the channel's delivery policy. The caller never
     * needs to handle provider errors; a receipt is always returned.
     */
    SendReceipt send(SendRequest request);

    /** Starts a conversation and sends its first message. */
    SendReceipt startConversation(String subject, SendRequest firstMessage);

    /** Sends a further message into an existing conversation. */
    SendReceipt reply(Long conversationId, SendRequest message);
}
