package com.civicdesk.module.grievance.controller;

import com.civicdesk.module.grievance.service.SupervisorGrievanceService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bean-validation coverage for the supervisor grievance endpoints: each invalid attribute
 * must yield 400 with a precise message and never reach the service.
 */
@WebMvcTest(SupervisorGrievanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class SupervisorGrievanceRequestValidationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SupervisorGrievanceService supervisorGrievanceService;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    // --- assignFieldOfficer ---

    @Test
    @WithMockUser(username = "sup-1", roles = "DS")
    void assign_blankFieldOfficerId_returns400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("fieldOfficerId", "");
        body.put("message", "Please inspect and fix.");
        mockMvc.perform(post("/grievance/assignFieldOfficer/g1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("fieldOfficerId is required")));
        verifyNoInteractions(supervisorGrievanceService);
    }

    @Test
    @WithMockUser(username = "sup-1", roles = "DS")
    void assign_blankMessage_returns400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("fieldOfficerId", "fo-1");
        body.put("message", "");
        mockMvc.perform(post("/grievance/assignFieldOfficer/g1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("message is required")));
        verifyNoInteractions(supervisorGrievanceService);
    }

    // --- resolveGrievance ---

    @Test
    @WithMockUser(username = "sup-1", roles = "DS")
    void resolve_blankMessage_returns400() throws Exception {
        mockMvc.perform(post("/grievance/resolveGrievance/g1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("message is required")));
        verifyNoInteractions(supervisorGrievanceService);
    }
}
