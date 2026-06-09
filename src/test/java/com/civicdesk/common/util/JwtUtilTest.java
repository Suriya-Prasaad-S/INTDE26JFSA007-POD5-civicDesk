package com.civicdesk.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void generateToken_validInputs_returnsNonNullToken() {
        String token = jwtUtil.generateToken("user-id", "CIT");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtil.generateToken("user-id", "CIT");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void extractRole_fromToken_returnsCorrectRole() {
        String token = jwtUtil.generateToken("user-id", "ADM");
        assertEquals("ADM", jwtUtil.extractRole(token));
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        assertFalse(jwtUtil.validateToken("this.is.not.a.valid.jwt"));
    }
}
