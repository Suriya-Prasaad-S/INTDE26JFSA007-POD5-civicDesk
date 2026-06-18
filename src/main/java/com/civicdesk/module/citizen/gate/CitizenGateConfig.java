package com.civicdesk.module.citizen.gate;

import com.civicdesk.module.citizen.repository.CitizenProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers {@link CitizenVerificationGate} as a servlet filter across all paths, ordered after the
 * Spring Security chain (so the security context is populated). The gate itself exempts the citizen
 * onboarding/account paths; everything else is gated for unverified citizens.
 *
 * <p>Registering via {@link FilterRegistrationBean} in a plain {@code @Configuration} (rather than a
 * {@code WebMvcConfigurer}/{@code @Component}) keeps the gate out of {@code @WebMvcTest} slices,
 * which have no JPA layer.
 */
@Configuration
public class CitizenGateConfig {

    @Bean
    public FilterRegistrationBean<CitizenVerificationGate> citizenVerificationGateRegistration(
            CitizenProfileRepository citizenRepository, ObjectMapper objectMapper) {
        FilterRegistrationBean<CitizenVerificationGate> registration =
                new FilterRegistrationBean<>(new CitizenVerificationGate(citizenRepository, objectMapper));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.LOWEST_PRECEDENCE); // after Spring Security
        return registration;
    }
}
