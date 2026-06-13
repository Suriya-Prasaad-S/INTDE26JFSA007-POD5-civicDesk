package com.civicdesk.common.exception.grievance;

/**
 * Thrown when an operation is not allowed in the grievance's current status
 * (e.g. editing when it is not Open, or closing/reopening when it is not
 * Resolved). Mapped to HTTP 409 by the global exception handler.
 */
public class InvalidGrievanceStateException extends RuntimeException {

    public InvalidGrievanceStateException(String message) {
        super(message);
    }
}
