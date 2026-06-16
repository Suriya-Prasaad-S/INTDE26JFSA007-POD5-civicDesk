package com.civicdesk.module.citizen.exception;

/**
 * Thrown when a request is well-formed but conflicts with the current state or a business rule —
 * e.g. an illegal status transition, or exceeding the "max 5 documents per citizen" limit.
 *
 * <p>Intended to map to <b>HTTP 409 Conflict</b> via the shared {@code common/}
 * GlobalExceptionHandler (Status &amp; Roadmap §3.1).
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
