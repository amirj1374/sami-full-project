package com.sami.app.common.exception;

import lombok.Getter;

/**
 * Base type for all deliberate, client-facing application errors.
 *
 * <p>Carrying an {@link ErrorCode} lets the global handler map any thrown
 * exception to a consistent HTTP status and response body without a growing
 * chain of {@code instanceof} checks.
 */
@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
