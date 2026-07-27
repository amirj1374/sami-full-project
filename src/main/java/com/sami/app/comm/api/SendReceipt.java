package com.sami.app.comm.api;

/**
 * What the caller gets back: the message row's identity and where delivery
 * stands. `duplicate` means the idempotency key matched an existing message —
 * the caller's send was absorbed, not re-delivered.
 */
public record SendReceipt(
        Long messageId,
        String messageNumber,
        String statusCode,
        String channelCode,
        boolean duplicate
) { }
