package com.sami.app.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Standard envelope returned by every endpoint so clients can rely on a single,
 * predictable shape.
 *
 * <pre>
 * {
 *   "success": true,
 *   "data": { ... },
 *   "error": null,
 *   "timestamp": "2026-07-17T10:15:30Z"
 * }
 * </pre>
 *
 * @param <T> the payload type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        Instant timestamp
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, Instant.now());
    }

    public static ApiResponse<Void> error(ApiError error) {
        return new ApiResponse<>(false, null, error, Instant.now());
    }

    /**
     * Machine-readable error detail. {@code fieldErrors} is only populated for
     * validation failures.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ApiError(
            String code,
            String message,
            List<FieldError> fieldErrors
    ) {
        public record FieldError(String field, String message) {
        }
    }
}
