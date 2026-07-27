package com.sami.app.comm.spi;

/**
 * Delivers messages for one provider technology.
 *
 * <p>Resolved from {@code comm_providers.handler_key}. A provider row whose
 * key has no bean is inert configuration — visible, editable, and dormant
 * until the bean ships. That contract is what lets the WhatsApp row exist
 * before any WhatsApp code does, and it is identical to the registries in the
 * files, automation and scheduling modules.
 *
 * <p>Handlers must never throw for delivery problems: report them through
 * {@link DeliveryResult} so the retry machinery can act on them. A thrown
 * exception is treated as a retryable infrastructure fault.
 */
public interface CommProviderHandler {

    /** Matches {@code comm_providers.handler_key}. */
    String key();

    DeliveryResult deliver(OutboundMessage message);
}
