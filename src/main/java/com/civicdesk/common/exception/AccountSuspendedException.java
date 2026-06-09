package com.civicdesk.common.exception;

/** Thrown when a suspended user attempts to log in (HTTP 423 Locked). */
public class AccountSuspendedException extends RuntimeException {
    public AccountSuspendedException(String message) {
        super(message);
    }
}
