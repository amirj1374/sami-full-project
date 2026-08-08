package com.sami.app.common.exception;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.api.ApiResponse.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.List;

/**
 * Translates exceptions into the standard {@link ApiResponse} envelope so the API
 * never leaks stack traces and always returns a predictable error shape.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Deliberate, typed application errors. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        ErrorCode code = ex.getErrorCode();
        return build(code, ex.getMessage(), null);
    }

    /** Bean-validation failures on {@code @Valid @RequestBody} arguments. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return build(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.defaultMessage(), fieldErrors);
    }

    /** Validation failures on path/query params ({@code @Validated} controllers). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerValidation(HandlerMethodValidationException ex) {
        return build(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.defaultMessage(), null);
    }

    /** Multipart parsing happens before a controller method is invoked. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return build(ErrorCode.BAD_REQUEST, "The uploaded file exceeds the allowed size", null);
    }

    /** Do not turn malformed multipart requests into an opaque HTTP 500. */
    @ExceptionHandler({MultipartException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ApiResponse<Void>> handleMultipart(Exception ex) {
        return build(ErrorCode.BAD_REQUEST, "The file upload request is invalid", null);
    }

    /** Wrong/missing credentials during authentication. */
    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        return build(ErrorCode.INVALID_CREDENTIALS, ErrorCode.INVALID_CREDENTIALS.defaultMessage(), null);
    }

    /** Authenticated but not authorized for the resource. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return build(ErrorCode.ACCESS_DENIED, ErrorCode.ACCESS_DENIED.defaultMessage(), null);
    }

    /** Catch-all: log with the real cause, return a generic 500 to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), null);
    }

    private ApiError.FieldError toFieldError(FieldError fe) {
        return new ApiError.FieldError(fe.getField(),
                fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value");
    }

    private ResponseEntity<ApiResponse<Void>> build(ErrorCode code, String message,
                                                    List<ApiError.FieldError> fieldErrors) {
        HttpStatus status = code.status();
        ApiError error = new ApiError(code.name(), message, fieldErrors);
        return ResponseEntity.status(status).body(ApiResponse.error(error));
    }
}
