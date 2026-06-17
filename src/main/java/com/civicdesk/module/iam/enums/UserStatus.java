package com.civicdesk.module.iam.enums;


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
