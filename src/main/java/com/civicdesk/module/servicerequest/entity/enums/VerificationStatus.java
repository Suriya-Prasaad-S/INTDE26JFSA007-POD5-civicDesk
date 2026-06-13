package com.civicdesk.module.serviceRequest.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Verification state of an uploaded request document.
 *
 * <p>In the JSON API each value is represented by its single-letter {@code code}
 * (Pending = {@code "P"}, Verified = {@code "V"}, Rejected = {@code "R"}). Request bodies
 * accept either the code or the full name.</p>
 */
public enum VerificationStatus {
    Pending("P"),
    Verified("V"),
    Rejected("R");

    private final String code;

    VerificationStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static VerificationStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (VerificationStatus status : values()) {
            if (status.code.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown verification status: " + value);
    }
}
