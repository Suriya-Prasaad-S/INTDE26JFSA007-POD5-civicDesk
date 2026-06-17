package com.civicdesk.common.exception;

/**
 * Thrown when a referenced entity (service, request, document, user) does not exist.
 * Mapped to HTTP 404 by the module exception handlers.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
