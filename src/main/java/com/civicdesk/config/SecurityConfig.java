// package com.civicdesk.config;

// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// import org.springframework.security.web.SecurityFilterChain;

// /**
//  * Temporary open security configuration.
//  *
//  * <p>spring-boot-starter-security is on the classpath, so without this bean Spring
//  * Security auto-secures every endpoint with HTTP Basic and a generated password —
//  * which would make the module's APIs return 401. Authentication for the CivicDesk
//  * platform is owned by the IAM module; until that lands, this permits all requests so
//  * the Service Request endpoints are testable. CSRF is disabled so POST / multipart
//  * uploads work without a token.</p>
//  *
//  * <p><b>Replace before production:</b> swap {@code permitAll()} for the real
//  * JWT-based authorization once IAM is integrated.</p>
//  */
// @Configuration
// @EnableWebSecurity
// public class SecurityConfig {

//     @Bean
//     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//         http
//                 .csrf(csrf -> csrf.disable())
//                 .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
//         return http.build();
//     }
// }
