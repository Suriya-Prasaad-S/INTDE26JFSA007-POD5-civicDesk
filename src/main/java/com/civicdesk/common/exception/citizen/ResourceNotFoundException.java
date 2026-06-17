package com.civicdesk.common.exception.citizen;

/**
 * Thrown when a requested citizen or document does not exist. Mapped to <b>HTTP 404</b> by the
 * shared {@code GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
