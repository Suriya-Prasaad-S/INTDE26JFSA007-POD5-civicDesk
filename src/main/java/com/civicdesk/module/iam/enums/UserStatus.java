package com.civicdesk.module.iam.enums;

/** Lifecycle states for a user account. The {@code label} (single char) is the value persisted in {@code users.status}. */
public enum UserStatus {
    ACT("A"),
    INA("I"),
    SUS("S");

    private final String label;

    UserStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Maps any accepted input form to the canonical single-char label, e.g.
     * "active"/"act"/"A" -> "A". Case-insensitive. Returns {@code null} for
     * unrecognised input so callers/validation can reject it.
     */
    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        switch (input.trim().toUpperCase()) {
            case "A": case "ACT": case "ACTIVE":
                return ACT.label;
            case "I": case "INA": case "INACTIVE":
                return INA.label;
            case "S": case "SUS": case "SUSPENDED":
                return SUS.label;
            default:
                return null;
        }
    }
}
