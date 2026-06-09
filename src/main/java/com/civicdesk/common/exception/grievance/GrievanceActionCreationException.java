package com.civicdesk.common.exception.grievance;

/**
 * Thrown when a grievance action could not be persisted because of an
 * unexpected data-access or runtime failure.
 */
public class GrievanceActionCreationException extends RuntimeException {

    public GrievanceActionCreationException(String message) {
        super(message);
    }

    public GrievanceActionCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
