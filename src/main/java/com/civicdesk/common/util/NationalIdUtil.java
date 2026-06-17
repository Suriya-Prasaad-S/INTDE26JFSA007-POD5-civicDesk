package com.civicdesk.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Helpers for handling a citizen's national ID (Aadhaar / Voter ID) safely:
 * the raw value is never stored — only a SHA-256 hash — and only the last four
 * digits are ever displayed.
 */
public final class NationalIdUtil {

    private NationalIdUtil() {
    }

    /** SHA-256 hex digest of the national ID, or {@code null} if blank. */
    public static String hash(String nationalId) {
        if (nationalId == null || nationalId.isBlank()) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(nationalId.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /** Masks all but the last four characters, e.g. {@code "********1234"}. */
    public static String maskLast4(String nationalId) {
        if (nationalId == null || nationalId.isBlank()) {
            return null;
        }
        String trimmed = nationalId.trim();
        if (trimmed.length() <= 4) {
            return trimmed;
        }
        String last4 = trimmed.substring(trimmed.length() - 4);
        return "*".repeat(trimmed.length() - 4) + last4;
    }
}
