package com.civicdesk.module.iam.controller;

import com.civicdesk.module.iam.security.JwtAuthFilter;
import com.civicdesk.module.auditlog.service.AuditService;
import com.civicdesk.module.iam.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bean-validation coverage for the auth request bodies: each invalid attribute must
 * yield 400 with a precise, attribute-named message and must never reach the service.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthRequestValidationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private AuditService auditService;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    /** A fully valid citizen registration payload; individual tests corrupt one field. */
    private Map<String, Object> validRegister() {
        Map<String, Object> m = new HashMap<>();
        m.put("name", "Ravi Kumar");
        m.put("email", "ravi@example.com");
        m.put("password", "Ravi@1234");
        m.put("phone", "9876543210");
        return m;
    }

    private void expectBadRequest(Map<String, Object> body, String messageFragment) throws Exception {
        mockMvc.perform(post("/iam/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(messageFragment)));
        verifyNoInteractions(authService);
    }

    @Test
    void register_validPayload_passesValidationAndReachesService() throws Exception {
        mockMvc.perform(post("/iam/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegister())))
                .andExpect(status().isCreated());
    }

    @Test
    void register_phoneNotTenDigits_returns400() throws Exception {
        Map<String, Object> body = validRegister();
        body.put("phone", "12345");
        expectBadRequest(body, "phone must be a valid 10-digit Indian mobile number");
    }

    @Test
    void register_phoneStartsBelowSix_returns400() throws Exception {
        Map<String, Object> body = validRegister();
        body.put("phone", "5876543210");
        expectBadRequest(body, "phone must be a valid 10-digit Indian mobile number");
    }

    @Test
    void register_malformedEmail_returns400() throws Exception {
        Map<String, Object> body = validRegister();
        body.put("email", "not-an-email");
        expectBadRequest(body, "email must be a valid email address");
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        Map<String, Object> body = validRegister();
        body.put("password", "short");
        expectBadRequest(body, "password must be between 8 and 72 characters");
    }

    @Test
    void register_invalidGenderWhenPresent_returns400() throws Exception {
        Map<String, Object> body = validRegister();
        body.put("gender", "ROBOT");
        expectBadRequest(body, "gender must be MALE, FEMALE or OTHER");
    }

    @Test
    void register_invalidDateOfBirthWhenPresent_returns400() throws Exception {
        Map<String, Object> body = validRegister();
        body.put("dateOfBirth", "01-01-1990");
        expectBadRequest(body, "dateOfBirth must be in format YYYY-MM-DD");
    }

    @Test
    void register_optionalProfileFieldsOmitted_isValid() throws Exception {
        // gender/dateOfBirth/nationalId/address/ward/zone are optional: omitting them must pass.
        mockMvc.perform(post("/iam/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegister())))
                .andExpect(status().isCreated());
    }

    @Test
    void staffLogin_malformedEmail_returns400() throws Exception {
        mockMvc.perform(post("/iam/auth/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "bad-email", "password", "whatever"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("email must be a valid email address")));
        verifyNoInteractions(authService);
    }

    @Test
    void setPassword_shortNewPassword_returns400() throws Exception {
        mockMvc.perform(post("/iam/auth/setPassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "user@example.com", "newPassword", "abc"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("newPassword must be between 8 and 72 characters")));
        verifyNoInteractions(authService);
    }
}
