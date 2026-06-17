package com.civicdesk.common.exception.citizen;

/**
 * Thrown when an operation would violate a uniqueness rule (e.g. a duplicate
 * {@code nationalIdNumber}). Mapped to <b>HTTP 409 Conflict</b> by the shared
 * {@code GlobalExceptionHandler}.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
