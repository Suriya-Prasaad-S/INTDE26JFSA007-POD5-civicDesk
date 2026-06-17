package com.civicdesk.module.iam.security;

import com.civicdesk.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validToken_setsAuthenticationAndContinues() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer good.token");
        when(jwtUtil.validateToken("good.token")).thenReturn(true);
        when(jwtUtil.extractRole("good.token")).thenReturn("ADM");
        when(jwtUtil.extractUserId("good.token")).thenReturn("u1");
        // Plenty of time left -> no refresh.
        when(jwtUtil.extractExpiration("good.token")).thenReturn(new Date(System.currentTimeMillis() + 1_800_000));

        new JwtAuthFilter(jwtUtil).doFilter(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("u1", auth.getPrincipal());
        assertEquals("ROLE_ADM", auth.getAuthorities().iterator().next().getAuthority());
        verify(chain).doFilter(request, response);
        // Token still fresh -> no new Authorization header.
        verify(response, never()).setHeader(org.mockito.ArgumentMatchers.eq("Authorization"), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void tokenNearExpiry_issuesFreshTokenOnResponseHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer aging.token");
        when(jwtUtil.validateToken("aging.token")).thenReturn(true);
        when(jwtUtil.extractRole("aging.token")).thenReturn("CIT");
        when(jwtUtil.extractUserId("aging.token")).thenReturn("u2");
        // Less than 10 minutes left -> refresh.
        when(jwtUtil.extractExpiration("aging.token")).thenReturn(new Date(System.currentTimeMillis() + 120_000));
        when(jwtUtil.generateToken("u2", "CIT")).thenReturn("fresh.token");

        new JwtAuthFilter(jwtUtil).doFilter(request, response, chain);

        verify(response).setHeader("Authorization", "Bearer fresh.token");
        verify(response).setHeader("Access-Control-Expose-Headers", "Authorization");
        verify(chain).doFilter(request, response);
    }

    @Test
    void missingHeader_passesThroughWithoutAuth() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        new JwtAuthFilter(jwtUtil).doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidToken_returns401AndStopsChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad");
        when(jwtUtil.validateToken("bad")).thenReturn(false);

        new JwtAuthFilter(jwtUtil).doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(request, response);
    }
}
