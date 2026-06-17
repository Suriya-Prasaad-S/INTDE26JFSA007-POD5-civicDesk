package com.civicdesk.module.citizen.support;

import java.security.SecureRandom;

/**
 * Generates random 16-character alphanumeric names for documents stored on disk (the on-disk file
 * name is generated, never derived from the user's file name, to prevent path traversal). Entity
 * ids are no longer produced here — {@code documentId} uses the shared numeric sequence generator
 * and a citizen profile is keyed by the IAM {@code userId}.
 *
 * <p>Effectively collision-free at this module's scale (62^16 ≈ 4.7e28 values, from a SecureRandom).
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
