package com.civicdesk.common.exception.grievance;

/**
 * Thrown when the data supplied to create a grievance or a grievance action
 * is missing or invalid (e.g. a required field is null or blank).
 */
public class InvalidGrievanceDataException extends RuntimeException {

    public InvalidGrievanceDataException(String message) {
        super(message);
    }

    public InvalidGrievanceDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
