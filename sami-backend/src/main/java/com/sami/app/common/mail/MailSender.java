package com.sami.app.common.mail;

/**
 * Outbound mail port.
 *
 * <p>The application depends only on this interface; swapping the logging
 * development implementation for a real SMTP/provider one is a single new bean.
 */
public interface MailSender {

    /**
     * Sends a password-reset message to the given address.
     *
     * @param email the recipient (a known account — callers enforce that)
     * @param token the plaintext reset token to embed in the reset link
     */
    void sendPasswordReset(String email, String token);
}
