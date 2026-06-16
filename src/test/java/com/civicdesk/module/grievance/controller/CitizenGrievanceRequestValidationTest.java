package com.civicdesk.module.grievance.controller;

import com.civicdesk.module.grievance.service.CitizenGrievanceService;
import com.civicdesk.module.iam.security.JwtAuthFilter;
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
 * Bean-validation coverage for the citizen grievance endpoints: each invalid attribute
 * must yield 400 with a precise message and never reach the service.
 */
@WebMvcTest(CitizenGrievanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class CitizenGrievanceRequestValidationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CitizenGrievanceService citizenGrievanceService;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    private Map<String, Object> validCreate() {
        Map<String, Object> m = new HashMap<>();
        m.put("category", "RI");
        m.put("grievanceTitle", "Pothole on main road");
        m.put("description", "Deep pothole near the bus stop");
        m.put("ward", "Ward 12");
        return m;
    }

    private void expectCreateBadRequest(Map<String, Object> body, String messageFragment) throws Exception {
        mockMvc.perform(post("/grievance/createGrievance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(messageFragment)));
        verifyNoInteractions(citizenGrievanceService);
    }

    // --- createGrievance ---

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void createGrievance_blankCategory_returns400() throws Exception {
        Map<String, Object> body = validCreate();
        body.put("category", "");
        expectCreateBadRequest(body, "category is required");
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void createGrievance_blankTitle_returns400() throws Exception {
        Map<String, Object> body = validCreate();
        body.put("grievanceTitle", "");
        expectCreateBadRequest(body, "grievanceTitle is required");
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void createGrievance_oversizeTitle_returns400() throws Exception {
        Map<String, Object> body = validCreate();
        body.put("grievanceTitle", "x".repeat(151));
        expectCreateBadRequest(body, "grievanceTitle must be at most 150 characters");
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void createGrievance_blankDescription_returns400() throws Exception {
        Map<String, Object> body = validCreate();
        body.put("description", "");
        expectCreateBadRequest(body, "description is required");
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void createGrievance_oversizeWard_returns400() throws Exception {
        Map<String, Object> body = validCreate();
        body.put("ward", "x".repeat(51));
        expectCreateBadRequest(body, "ward must be at most 50 characters");
    }

    // --- updateGrievanceDetails ---

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void updateDetails_blankTitle_returns400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("grievanceTitle", "");
        body.put("description", "still here");
        mockMvc.perform(put("/grievance/updateGrievanceDetails/g1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("grievanceTitle is required")));
        verifyNoInteractions(citizenGrievanceService);
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void updateDetails_blankDescription_returns400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("grievanceTitle", "A title");
        body.put("description", "");
        mockMvc.perform(put("/grievance/updateGrievanceDetails/g1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("description is required")));
        verifyNoInteractions(citizenGrievanceService);
    }

    // --- reopenGrievance ---

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void reopen_blankReason_returns400() throws Exception {
        mockMvc.perform(post("/grievance/reopenGrievance/g1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("reason is required")));
        verifyNoInteractions(citizenGrievanceService);
    }
}
