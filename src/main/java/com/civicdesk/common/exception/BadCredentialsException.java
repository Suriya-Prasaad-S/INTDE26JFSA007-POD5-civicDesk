package com.civicdesk.common.exception;

/** Thrown when login fails due to a wrong email or password (HTTP 401). */
public class BadCredentialsException extends RuntimeException {
    public BadCredentialsException(String message) {
        super(message);
    }
}
