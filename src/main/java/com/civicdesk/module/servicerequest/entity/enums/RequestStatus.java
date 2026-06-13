package com.civicdesk.module.serviceRequest.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.EnumSet;
import java.util.Set;

/**
 * Workflow stage of a service request, with the allowed transitions between stages.
 *
 * <p>The workflow is:</p>
 * <pre>
 *   Submitted ──▶ UnderReview ──▶ Approved ──▶ Completed
 *                     │  ▲           │
 *                     ▼  │           ▼
 *              PendingDocuments    Rejected (terminal)
 * </pre>
 * Rejected and Completed are terminal. A document rejection (see verifyDocument) moves an
 * in-flight request back to PendingDocuments so the citizen can re-upload.
 *
 * <p>In the JSON API each value is represented by its single-letter {@code code}
 * (Submitted = {@code "S"}, UnderReview = {@code "U"}, PendingDocuments = {@code "P"},
 * Approved = {@code "A"}, Rejected = {@code "R"}, Completed = {@code "C"}). Request bodies
 * accept either the code or the full name.</p>
 */
public enum RequestStatus {
    Submitted("S"),
    UnderReview("U"),
    PendingDocuments("P"),
    Approved("A"),
    Rejected("R"),
    Completed("C");

    private final String code;

    RequestStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static RequestStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (RequestStatus status : values()) {
            if (status.code.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown request status: " + value);
    }

    /** Terminal states accept no further transitions and no new document uploads. */
    public boolean isTerminal() {
        return this == Rejected || this == Completed;
    }

    /** The statuses this status may legally transition to. */
    public Set<RequestStatus> allowedNextStates() {
        return switch (this) {
            case Submitted -> EnumSet.of(UnderReview);
            case UnderReview -> EnumSet.of(PendingDocuments, Approved, Rejected);
            case PendingDocuments -> EnumSet.of(UnderReview, Rejected);
            case Approved -> EnumSet.of(Completed, Rejected);
            case Rejected, Completed -> EnumSet.noneOf(RequestStatus.class);
        };
    }
}
