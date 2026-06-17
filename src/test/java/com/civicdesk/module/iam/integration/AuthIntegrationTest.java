package com.civicdesk.module.iam.integration;

import com.civicdesk.module.iam.repository.DepartmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full stack: register -> citizen login -> use JWT on a protected endpoint. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper om;
    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    void registerThenLoginThenAccessProtectedEndpoint() throws Exception {
        String email = "ravi.auth.it@example.com";

        mockMvc.perform(post("/iam/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "name", "Ravi", "email", email, "password", "Ravi@1234",
                                "phone", "9876543210"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.data").doesNotExist());

        String loginBody = mockMvc.perform(post("/iam/auth/citizen/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email, "password", "Ravi@1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiresIn").value(1800))
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(loginBody, "$.data.token");

        mockMvc.perform(get("/iam/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.data.email").value(email));
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/iam/users/me")).andExpect(status().isUnauthorized());
    }

    /** Admin creates a supervisor (no password) -> login blocked -> set-password -> login works. */
    @Test
    void adminCreatedStaff_mustSetPasswordBeforeLogin() throws Exception {
        String adminToken = JsonPath.read(mockMvc.perform(post("/iam/auth/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "email", "admin@civicdesk.gov", "password", "Admin@12345"))))
                .andReturn().getResponse().getContentAsString(), "$.data.token");

        String deptId = departmentRepository.findAll().get(0).getDepartmentId();
        String email = "priya.sup.it@civicdesk.gov";

        mockMvc.perform(post("/iam/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "name", "Priya", "email", email, "phone", "9876543210",
                                "role", "DS", "departmentId", deptId))))
                .andExpect(status().isCreated());

        // 1) Login before setting a password -> 403
        mockMvc.perform(post("/iam/auth/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email, "password", "anything"))))
                .andExpect(status().isForbidden());

        // 2) Set password (email + newPassword only) -> 200
        mockMvc.perform(post("/iam/auth/setPassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "email", email, "newPassword", "Priya@1234"))))
                .andExpect(status().isOk());

        // 3) Now login works -> 200 with JWT
        mockMvc.perform(post("/iam/auth/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email, "password", "Priya@1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("DS"));

        // 4) set-password again -> 403 (one-time only)
        mockMvc.perform(post("/iam/auth/setPassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "email", email, "newPassword", "Priya@1234"))))
                .andExpect(status().isForbidden());
    }
}
