package com.civicdesk.common.exception.citizen;

/**
 * Thrown when the caller is not permitted to perform an action — e.g. a citizen accessing another
 * citizen's documents. Mapped to <b>HTTP 403 Forbidden</b> by the shared
 * {@code GlobalExceptionHandler}.
 */
public class ForbiddenActionException extends RuntimeException {
    public ForbiddenActionException(String message) {
        super(message);
    }
}
