package com.civicdesk.common.exception;

/**
 * Thrown when a request conflicts with the current state of a resource, such as
 * creating a catalog service whose name already exists.
 * Mapped to HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
