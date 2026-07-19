package com.sami.app.portal.spi;

/**
 * Delivers a one-time code to a customer.
 *
 * <p>No implementation ships with this module. That is deliberate: the ERP has
 * no SMS gateway and its only mail sender writes to the log, so an OTP issued
 * today could only be delivered by printing it. {@code PortalOtpService}
 * therefore REFUSES to issue a code when no channel is registered rather than
 * leaking it — see that class.
 *
 * <p>Adding SMS is a bean implementing this interface plus enabling the
 * {@code otp-sms} row; no change to the portal.
 */
public interface OtpDeliveryChannel {

    /** Matches {@code portal_auth_methods.handler_key}, e.g. {@code otp-sms}. */
    String key();

    /**
     * @param target the destination — mobile number or email address
     * @param code   the plaintext code; implementations must not log it
     */
    void deliver(String target, String code, String language);
}
