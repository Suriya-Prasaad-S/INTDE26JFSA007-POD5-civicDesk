package com.civicdesk.common.exception.citizen;

/**
 * Thrown when request content is invalid in a way the Bean Validation annotations cannot express —
 * e.g. an unknown enum/status code ({@code gender}/{@code status}/{@code documentType}), an update
 * with no fields, or a file that is empty, too large, or of an unsupported type. Mapped to
 * <b>HTTP 400 Bad Request</b> by the shared {@code GlobalExceptionHandler}.
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
