package com.sami.app.common.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Development stand-in for a real mail provider: writes the message to the
 * application log instead of sending it, so the reset flow is fully testable
 * without SMTP credentials.
 */
@Slf4j
@Component
public class LoggingMailSender implements MailSender {

    @Override
    public void sendPasswordReset(String email, String token) {
        log.info("Password reset requested for {} — reset token: {} (link: /auth/reset-password?token={})",
                email, token, token);
    }
}
