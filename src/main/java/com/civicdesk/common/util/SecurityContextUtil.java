package com.civicdesk.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reads the current authenticated principal that {@code JwtAuthFilter} placed in
 * the {@link SecurityContextHolder}: the userId (principal) and role (single
 * {@code ROLE_*} authority, prefix stripped).
 */
public final class SecurityContextUtil {

    private SecurityContextUtil() {
    }

    /** The current user's id (JWT {@code userId} claim), or {@code null} if unauthenticated. */
    public static String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        return auth.getPrincipal().toString();
    }

    /** The current user's role without the {@code ROLE_} prefix, or {@code null} if none. */
    public static String getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().isEmpty()) {
            return null;
        }
        return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }
}
