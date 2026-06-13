package com.civicdesk.common.exception;

/**
 * Thrown for malformed requests not covered by bean validation, such as an
 * unsupported document file type. Mapped to HTTP 400 by {@link GlobalExceptionHandler}.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
