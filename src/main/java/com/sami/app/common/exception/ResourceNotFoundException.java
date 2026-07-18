package com.sami.app.common.exception;

/** Thrown when a requested entity does not exist. Maps to HTTP 404. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    /** Convenience factory, e.g. {@code ResourceNotFoundException.of("Product", 42)}. */
    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException("%s with id %s was not found".formatted(resource, id));
    }
}
