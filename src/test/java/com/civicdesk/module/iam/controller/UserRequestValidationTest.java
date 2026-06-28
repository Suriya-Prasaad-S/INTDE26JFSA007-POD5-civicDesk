package com.civicdesk.module.iam.controller;

import com.civicdesk.module.iam.security.JwtAuthFilter;
import com.civicdesk.module.auditlog.service.AuditService;
import com.civicdesk.module.iam.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bean-validation coverage for {@code POST /users} and {@code PUT /users/{id}/status}:
 * each invalid attribute must yield 400 with a precise message and never reach the service.
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserRequestValidationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private AuditService auditService;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    private Map<String, Object> validCreate() {
        Map<String, Object> m = new HashMap<>();
        m.put("name", "Meena");
        m.put("email", "meena@civicdesk.gov");
        m.put("phone", "9876543210");
        m.put("role", "DS");
        m.put("departmentId", "DPT01");
        return m;
    }

    private void expectCreateBadRequest(Map<String, Object> body, String messageFragment) throws Exception {
        mockMvc.perform(post("/iam/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(messageFragment)));
        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "admin-id", roles = "ADM")
    void createUser_invalidPhone_returns400() throws Exception {
        Map<String, Object> body = validCreate();
        body.put("phone", "0000000000");
        expectCreateBadRequest(body, "phone must be a valid 10-digit Indian mobile number");
    }

    @Test
    @WithMockUser(username = "admin-id", roles = "ADM")
    void createUser_blankPhone_returns400() throws Exception {
        Map<String, Object> body = validCreate();
        body.put("phone", "");
        expectCreateBadRequest(body, "phone is required");
    }

    @Test
    @WithMockUser(username = "admin-id", roles = "ADM")
    void createUser_unknownRole_returns400() throws Exception {
        Map<String, Object> body = validCreate();
        body.put("role", "WIZARD");
        expectCreateBadRequest(body, "role must be one of: DS, FO, ENG, CO");
    }

    @Test
    @WithMockUser(username = "admin-id", roles = "ADM")
    void createUser_malformedEmail_returns400() throws Exception {
        Map<String, Object> body = validCreate();
        body.put("email", "nope");
        expectCreateBadRequest(body, "email must be a valid email address");
    }

    @Test
    @WithMockUser(username = "admin-id", roles = "ADM")
    void createUser_blankName_returns400() throws Exception {
        Map<String, Object> body = validCreate();
        body.put("name", "");
        expectCreateBadRequest(body, "name is required");
    }

    @Test
    @WithMockUser(username = "admin-id", roles = "ADM")
    void updateStatus_invalidStatus_returns400() throws Exception {
        mockMvc.perform(put("/iam/users/10000002/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "BOGUS"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("status must be one of")));
        verifyNoInteractions(userService);
    }
}
