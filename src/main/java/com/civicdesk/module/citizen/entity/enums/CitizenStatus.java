package com.civicdesk.module.citizen.entity.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Lifecycle status of a citizen profile, persisted and exposed as a single-character code
 * ({@code A} = Active, {@code V} = Verified, {@code F} = Flagged). Default on registration is
 * {@link #Active}.
 *
 * <p>Allowed transitions (enforced in the service layer):
 * Active &rarr; Verified, Active &harr; Flagged, Verified &rarr; Flagged.
 */
public enum CitizenStatus {
    Active("A"),
    Verified("V"),
    Flagged("F");

    private final String code;

    CitizenStatus(String code) {
        this.code = code;
    }

    /** The single-character code stored in the DB and used on the API. */
    public String getCode() {
        return code;
    }

    /** Maps a single-character code (case-insensitive) to its constant. */
    public static CitizenStatus fromCode(String code) {
        for (CitizenStatus s : values()) {
            if (s.code.equalsIgnoreCase(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown CitizenStatus code: " + code);
    }

    /** Comma-joined list of allowed codes, for precise error messages. */
    public static String allowedCodes() {
        return Arrays.stream(values()).map(s -> s.code).collect(Collectors.joining(", "));
    }
}
