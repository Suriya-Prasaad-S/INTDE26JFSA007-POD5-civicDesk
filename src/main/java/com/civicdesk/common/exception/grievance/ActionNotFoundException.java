package com.civicdesk.common.exception.grievance;

/** Thrown when no grievance action exists for the given id. Mapped to HTTP 404. */
public class ActionNotFoundException extends RuntimeException {
    public ActionNotFoundException(String message) {
        super(message);
    }
}
