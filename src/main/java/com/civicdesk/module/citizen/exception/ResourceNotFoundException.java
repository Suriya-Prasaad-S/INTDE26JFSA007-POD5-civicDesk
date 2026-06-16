package com.civicdesk.module.citizen.exception;

/**
 * Thrown when a requested citizen or document does not exist.
 *
 * <p>Intended to map to <b>HTTP 404</b>. The mapping itself will live in the shared
 * {@code common/} GlobalExceptionHandler once that module lands (Status &amp; Roadmap §3.1);
 * until then this is a plain module-local runtime exception so the service layer stays
 * decoupled from the (undecided) shared response contract and remains unit-testable on its own.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
