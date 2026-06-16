package com.civicdesk.module.citizen.exception;

/**
 * Thrown when request content is invalid in a way the Bean Validation annotations cannot express —
 * e.g. an unknown enum/status code ({@code gender}/{@code status}/{@code documentType}), an update
 * with no fields, or a file that is empty, too large, or of an unsupported type.
 *
 * <p>Intended to map to <b>HTTP 400 Bad Request</b> (handled by the module's
 * {@code CitizenExceptionHandler}).
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
