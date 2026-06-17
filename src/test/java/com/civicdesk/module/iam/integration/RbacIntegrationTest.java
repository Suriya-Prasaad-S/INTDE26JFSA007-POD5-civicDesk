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

/** Full stack RBAC: seeded ADM vs a CIT hitting allowed/forbidden endpoints. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper om;
    @Autowired
    private DepartmentRepository departmentRepository;

    private String adminToken() throws Exception {
        // Seeded by DataSeeder using the local-dev defaults.
        String body = mockMvc.perform(post("/iam/auth/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "email", "admin@civicdesk.gov", "password", "Admin@12345"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.token");
    }

    private String citizenToken(String email) throws Exception {
        mockMvc.perform(post("/iam/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of(
                        "name", "Cit", "email", email, "password", "Cit@1234",
                        "phone", "9876543210"))));
        String body = mockMvc.perform(post("/iam/auth/citizen/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email, "password", "Cit@1234"))))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.token");
    }

    @Test
    void admin_canListUsers() throws Exception {
        mockMvc.perform(get("/iam/users").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void citizen_cannotListUsers_returns403() throws Exception {
        String token = citizenToken("cit.rbac.it@example.com");
        mockMvc.perform(get("/iam/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_createsSupervisor_returns201() throws Exception {
        // Use a real seeded department id (createUser validates it exists).
        String departmentId = departmentRepository.findAll().get(0).getDepartmentId();

        mockMvc.perform(post("/iam/users")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "name", "Meena",
                                "email", "sup.rbac.it@civicdesk.gov",
                                "phone", "9876543210",
                                "role", "DS",
                                "departmentId", departmentId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void admin_createsSupervisor_withUnknownDepartment_returns400() throws Exception {
        mockMvc.perform(post("/iam/users")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "name", "Ghost",
                                "email", "ghost.sup.rbac.it@civicdesk.gov",
                                "phone", "9876543210",
                                "role", "DS",
                                "departmentId", "no-such-department"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/iam/users")).andExpect(status().isUnauthorized());
    }
}
