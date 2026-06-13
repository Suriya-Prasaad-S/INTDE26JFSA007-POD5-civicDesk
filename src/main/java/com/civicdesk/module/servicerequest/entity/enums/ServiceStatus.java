package com.civicdesk.module.serviceRequest.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Lifecycle status of a catalog service. Inactive services reject new requests.
 *
 * <p>In the JSON API each value is represented by its single-letter {@code code}
 * (Active = {@code "A"}, Inactive = {@code "I"}). Request bodies accept either the code
 * or the full name.</p>
 */
public enum ServiceStatus {
    Active("A"),
    Inactive("I");

    private final String code;

    ServiceStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static ServiceStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (ServiceStatus status : values()) {
            if (status.code.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown service status: " + value);
    }
}
