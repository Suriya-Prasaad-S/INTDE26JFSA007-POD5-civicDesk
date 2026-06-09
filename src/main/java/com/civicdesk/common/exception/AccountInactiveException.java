package com.civicdesk.common.exception;

/** Thrown when an inactive (deactivated) user attempts to log in (HTTP 403 Forbidden). */
public class AccountInactiveException extends RuntimeException {
    public AccountInactiveException(String message) {
        super(message);
    }
}
