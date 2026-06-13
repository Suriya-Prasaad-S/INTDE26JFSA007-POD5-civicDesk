package com.civicdesk.common.exception;

/**
 * Thrown when a request is well-formed but violates a business rule (e.g. submitting
 * against an Inactive service, or uploading to a terminal request).
 * Mapped to HTTP 422 by {@link GlobalExceptionHandler}.
 */
public class UnprocessableEntityException extends RuntimeException {

    public UnprocessableEntityException(String message) {
        super(message);
    }
}
