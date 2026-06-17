package com.civicdesk.common.exception;

/** Thrown when an email is already registered (HTTP 409). */
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) {
        super(message);
    }
}
