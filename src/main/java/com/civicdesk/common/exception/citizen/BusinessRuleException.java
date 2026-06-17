package com.civicdesk.common.exception.citizen;

/**
 * Thrown when a request is well-formed but conflicts with the current state or a business rule —
 * e.g. an illegal status transition, completing a profile before verification, or exceeding the
 * "max 5 documents per citizen" limit. Mapped to <b>HTTP 409 Conflict</b> by the shared
 * {@code GlobalExceptionHandler}.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
