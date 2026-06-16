package com.civicdesk.module.citizen.exception;

/**
 * Thrown when registration would violate a uniqueness rule (duplicate {@code email} or
 * {@code nationalIdNumber}).
 *
 * <p>Intended to map to <b>HTTP 409 Conflict</b> via the shared {@code common/}
 * GlobalExceptionHandler (Status &amp; Roadmap §3.1).
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
