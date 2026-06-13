package com.civicdesk.common.exception.grievance;

/**
 * Thrown when a grievance action cannot be edited — it is not a WORK action, not the
 * latest action, already Completed, or the caller is not its creator. Mapped to HTTP 409.
 */
public class ActionNotEditableException extends RuntimeException {
    public ActionNotEditableException(String message) {
        super(message);
    }
}
