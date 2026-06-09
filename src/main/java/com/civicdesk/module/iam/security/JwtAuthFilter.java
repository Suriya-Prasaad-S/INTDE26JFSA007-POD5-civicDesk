package com.civicdesk.module.iam.security;

import com.civicdesk.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads the {@code Authorization: Bearer <jwt>} header, validates the token and
 * populates the {@link SecurityContextHolder} with the userId as principal and a
 * {@code ROLE_<role>} authority so {@code @PreAuthorize} guards can act on it.
 * A missing/expired/invalid token yields 401.
 *
 * <p>Sliding expiry: when the current token has 10 minutes (600 s) or less left,
 * a fresh 30-minute token is issued and returned on the {@code Authorization}
 * response header (exposed via {@code Access-Control-Expose-Headers}).
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Refresh the token once it has this many seconds or fewer remaining. */
    private static final long REFRESH_THRESHOLD_SECONDS = 600;

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }

        String token = header.substring(7);
        if (!jwtUtil.validateToken(token)) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String role = jwtUtil.extractRole(token);
        String userId = jwtUtil.extractUserId(token);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        userId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));

        SecurityContextHolder.getContext().setAuthentication(auth);

        // Sliding expiry: issue a fresh token only when close to expiring.
        long remaining = (jwtUtil.extractExpiration(token).getTime() - System.currentTimeMillis()) / 1000;
        if (remaining <= REFRESH_THRESHOLD_SECONDS) {
            String freshToken = jwtUtil.generateToken(userId, role);
            res.setHeader("Authorization", "Bearer " + freshToken);
            res.setHeader("Access-Control-Expose-Headers", "Authorization");
        }

        chain.doFilter(req, res);
    }
}
