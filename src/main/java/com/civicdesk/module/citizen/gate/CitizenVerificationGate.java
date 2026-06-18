package com.civicdesk.module.citizen.gate;

import com.civicdesk.common.response.ApiResponse;
import com.civicdesk.common.util.SecurityContextUtil;
import com.civicdesk.module.citizen.entity.enums.CitizenStatus;
import com.civicdesk.module.citizen.repository.CitizenProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Blocks a citizen ({@code ROLE_CIT}) from using application services until their
 * {@link CitizenStatus#Verified} status is reached. Implemented as a servlet filter (registered by
 * {@code CitizenGateConfig}) ordered after the Spring Security chain, so the authenticated
 * principal is available. Non-citizen callers are never gated; an {@code Active} (registered but
 * not yet verified) or {@code Flagged} citizen is rejected with {@code 403}.
 *
 * <p>Exempt paths (still reachable while unverified): the citizen's own profile/wallet
 * ({@code /citizenProfile/**}), IAM account/login ({@code /iam/**}), and the API docs.
 */
public class CitizenVerificationGate extends OncePerRequestFilter {

    private final CitizenProfileRepository citizenRepository;
    private final ObjectMapper objectMapper;

    public CitizenVerificationGate(CitizenProfileRepository citizenRepository, ObjectMapper objectMapper) {
        this.citizenRepository = citizenRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());

        // Only citizens are gated, and only outside the exempt onboarding/account paths.
        if (isExempt(path) || !"CIT".equals(SecurityContextUtil.getCurrentRole())) {
            chain.doFilter(request, response);
            return;
        }

        String userId = SecurityContextUtil.getCurrentUserId();
        boolean verified = userId != null && citizenRepository.findById(userId)
                .map(profile -> profile.getStatus() == CitizenStatus.Verified)
                .orElse(false);
        if (verified) {
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(
                "Your account is not verified yet. Complete verification to use this service.")));
    }

    private static boolean isExempt(String path) {
        return path.startsWith("/citizenProfile")
                || path.startsWith("/iam")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
}
