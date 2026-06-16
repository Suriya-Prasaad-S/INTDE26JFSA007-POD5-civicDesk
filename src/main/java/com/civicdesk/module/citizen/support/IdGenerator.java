package com.civicdesk.module.citizen.support;

import java.security.SecureRandom;

/**
 * Generates 16-character alphanumeric identifiers for {@code citizenId} / {@code documentId}.
 *
 * <p>Replaces the earlier CHAR(36) UUID v4: shorter and human-friendlier, while still effectively
 * collision-free at this module's scale (62^16 ≈ 4.7e28 possible values, drawn from a SecureRandom).
 */
public final class IdGenerator {

    /** Length of a generated id. */
    public static final int ID_LENGTH = 16;

    private static final char[] ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private IdGenerator() {
    }

    /** A fresh 16-character alphanumeric id. */
    public static String newId() {
        StringBuilder sb = new StringBuilder(ID_LENGTH);
        for (int i = 0; i < ID_LENGTH; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
