package com.civicdesk.common.util;

import com.civicdesk.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Issues and verifies stateless HS256 JWTs. Shared with ALL teammates — every
 * module's auth filter relies on these claims: {@code userId} and {@code role}.
 * Secret and expiry come from {@link JwtConfig}.
 */
@Component
public class JwtUtil {

    private final JwtConfig jwtConfig;

    public JwtUtil(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    public String generateToken(String userId, String role) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiry()))
                .signWith(getKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims extractClaims(String token) {
        return Jwts.parser().verifyWith(getKey()).build()
                .parseSignedClaims(token).getPayload();
    }

    public String extractUserId(String token) {
        return extractClaims(token).get("userId", String.class);
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    /** Token expiry instant — used by the sliding-expiry refresh in JwtAuthFilter. */
    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
