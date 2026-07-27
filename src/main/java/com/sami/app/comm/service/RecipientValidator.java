package com.sami.app.comm.service;

import com.sami.app.comm.domain.CommChannelType;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Validates a recipient address against what the channel TYPE requires —
 * flags, never channel names, decide which shape applies.
 *
 * <p>Format checks are deliberately lenient (syntax, not deliverability): the
 * provider is the final authority, and its verdict comes back through
 * DeliveryResult as a permanent failure. What this stops is the obvious
 * garbage — an email address on an SMS channel — before a message row exists.
 */
@Component
public class RecipientValidator {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    /** Digits with optional leading +, 7–15 digits (E.164 ceiling). */
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{7,15}$");

    public void validate(CommChannelType type, String address) {
        if (address == null || address.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Recipient address is required");
        }
        if (type.isRequiresEmail() && !EMAIL.matcher(address).matches()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "'%s' is not a valid email address for channel type %s".formatted(address, type.getCode()));
        }
        if (type.isRequiresPhone() && !PHONE.matcher(normalizePhone(address)).matches()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "'%s' is not a valid phone number for channel type %s".formatted(address, type.getCode()));
        }
        if (type.isRequiresDeviceToken() && address.length() < 16) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "A device token is required for channel type %s".formatted(type.getCode()));
        }
    }

    /** Strips spaces and dashes; converts a leading 00 to +. */
    public String normalizePhone(String raw) {
        String cleaned = raw.replaceAll("[\\s-]", "");
        if (cleaned.startsWith("00")) {
            cleaned = "+" + cleaned.substring(2);
        }
        return cleaned;
    }
}
