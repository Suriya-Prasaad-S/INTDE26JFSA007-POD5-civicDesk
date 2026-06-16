package com.civicdesk.module.citizen.entity.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Lifecycle status of a citizen document, persisted and exposed as a single-character code
 * ({@code V} = Valid, {@code E} = Expired, {@code R} = Revoked). Default on upload is {@link #Valid}.
 *
 * <p>Transitions: Valid &rarr; Expired (automatic, when past expiryDate),
 * Valid &rarr; Revoked and Expired &rarr; Revoked (manual, by a Department Supervisor).
 */
public enum DocumentStatus {
    Valid("V"),
    Expired("E"),
    Revoked("R");

    private final String code;

    DocumentStatus(String code) {
        this.code = code;
    }

    /** The single-character code stored in the DB and used on the API. */
    public String getCode() {
        return code;
    }

    /** Maps a single-character code (case-insensitive) to its constant. */
    public static DocumentStatus fromCode(String code) {
        for (DocumentStatus s : values()) {
            if (s.code.equalsIgnoreCase(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown DocumentStatus code: " + code);
    }

    /** Comma-joined list of allowed codes, for precise error messages. */
    public static String allowedCodes() {
        return Arrays.stream(values()).map(s -> s.code).collect(Collectors.joining(", "));
    }
}
