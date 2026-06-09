package com.civicdesk.common.exception;

/** Thrown when a request is structurally valid but semantically invalid (HTTP 400). */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
