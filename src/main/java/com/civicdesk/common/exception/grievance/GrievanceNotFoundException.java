package com.civicdesk.common.exception.grievance;

/**
 * Thrown when a grievance referenced by an operation (for example while
 * creating a grievance action) cannot be found in the system.
 */
public class GrievanceNotFoundException extends RuntimeException {

    public GrievanceNotFoundException(String message) {
        super(message);
    }

    public GrievanceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
