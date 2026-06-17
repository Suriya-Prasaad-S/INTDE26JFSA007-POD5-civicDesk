package com.civicdesk.common.exception;

/**
 * Thrown when a user who hasn't set a password yet (admin-created account)
 * attempts to log in (HTTP 403). The client should redirect to the
 * set-password page.
 */
public class PasswordNotSetException extends RuntimeException {
    public PasswordNotSetException(String message) {
        super(message);
    }
}
