package com.civicdesk.common.exception;

/** Thrown when a JWT has expired (HTTP 401). */
public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
}
