package com.civicdesk.module.citizen.exception;

/**
 * Thrown when the caller is not permitted to perform an action — e.g. a non–Department-Supervisor
 * attempting to verify a document. Intended to map to <b>HTTP 403</b>.
 *
 * <p>This is a temporary stand-in for the Tier-3 role check (JWT + {@code @PreAuthorize}) that
 * Module 2.1 (IAM) will provide; until then the verifier's role is looked up in the {@code users}
 * table by the service layer.
 */
public class ForbiddenActionException extends RuntimeException {
    public ForbiddenActionException(String message) {
        super(message);
    }
}
