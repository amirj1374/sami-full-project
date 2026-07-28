package com.sami.app.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Central catalogue of application error codes.
 *
 * <p>Codes are stable, client-facing identifiers (safe to switch on in the
 * frontend), decoupled from human-readable messages and HTTP status.
 */
public enum ErrorCode {

    // 400
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad request"),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "Current password is incorrect"),
    // 401
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid or expired token"),
    // 403
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),
    // 404
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    // 409
    RESOURCE_CONFLICT(HttpStatus.CONFLICT, "Resource conflict"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email is already registered"),
    OPERATION_NOT_ALLOWED(HttpStatus.CONFLICT, "This operation is not allowed"),
    // 500
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
