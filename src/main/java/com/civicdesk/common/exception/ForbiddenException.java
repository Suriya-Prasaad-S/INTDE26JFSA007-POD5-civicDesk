package com.civicdesk.common.exception;

/**
 * Thrown when an actor is not permitted to perform an action (e.g. a flagged citizen
 * submitting a request, or accessing another citizen's data).
 * Mapped to HTTP 403 by {@link GlobalExceptionHandler}.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
