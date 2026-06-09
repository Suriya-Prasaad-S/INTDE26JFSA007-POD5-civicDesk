package com.civicdesk.common.exception.grievance;

/**
 * Thrown when a grievance query is requested for a role that the system does
 * not recognise (the supported roles are CITIZEN, FIELD_OFFICER,
 * DEPARTMENT_SUPERVISOR and ADMIN).
 */
public class InvalidUserRoleException extends RuntimeException {

    public InvalidUserRoleException(String message) {
        super(message);
    }

    public InvalidUserRoleException(String message, Throwable cause) {
        super(message, cause);
    }
}
