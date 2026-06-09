package com.civicdesk.common.exception;

/** Thrown when an authenticated caller is not allowed to perform an action (HTTP 403). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
