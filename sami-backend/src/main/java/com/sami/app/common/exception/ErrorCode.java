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
    LEGACY_IMPORT_FILE_REQUIRED(HttpStatus.BAD_REQUEST, "Select a non-empty Asan migration file"),
    LEGACY_IMPORT_UNSUPPORTED_FORMAT(HttpStatus.BAD_REQUEST, "The selected file is not a supported Asan migration file"),
    LEGACY_IMPORT_UNSAFE_ARCHIVE(HttpStatus.BAD_REQUEST, "The selected archive did not pass the security checks"),
    LEGACY_IMPORT_EMPTY(HttpStatus.BAD_REQUEST, "The selected file contains no readable migration data"),
    LEGACY_IMPORT_MANIFEST_REQUIRED(HttpStatus.BAD_REQUEST, "The migration package manifest is missing or invalid"),
    LEGACY_IMPORT_UNREADABLE(HttpStatus.UNPROCESSABLE_ENTITY, "The selected migration file could not be read"),
    LEGACY_IMPORT_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "The selected migration file exceeds the allowed limits"),
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
    LEGACY_IMPORT_DUPLICATE(HttpStatus.CONFLICT, "This migration file has already been uploaded"),
    LEGACY_IMPORT_INVALID_STATE(HttpStatus.CONFLICT, "This migration operation is not available in the current state"),
    // 413
    UPLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "The uploaded file exceeds the allowed size"),
    // 503
    LEGACY_IMPORT_TOOL_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The server tool required to read this archive is unavailable"),
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
