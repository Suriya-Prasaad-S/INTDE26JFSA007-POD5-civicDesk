package com.civicdesk.module.grievance.controller;

import com.civicdesk.module.grievance.service.FieldOfficerGrievanceService;
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
 * Bean-validation coverage for the field-officer grievance endpoints: each invalid attribute
 * must yield 400 with a precise message and never reach the service.
 */
@WebMvcTest(FieldOfficerGrievanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class FieldOfficerGrievanceRequestValidationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FieldOfficerGrievanceService fieldOfficerGrievanceService;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    // --- createGrievanceAction ---

    @Test
    @WithMockUser(username = "fo-1", roles = "FO")
    void createAction_blankTitle_returns400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("grievanceActionTitle", "");
        body.put("actionDescription", "Inspected the location");
        mockMvc.perform(post("/grievance/createGrievanceAction/g1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("grievanceActionTitle is required")));
        verifyNoInteractions(fieldOfficerGrievanceService);
    }

    @Test
    @WithMockUser(username = "fo-1", roles = "FO")
    void createAction_blankDescription_returns400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("grievanceActionTitle", "Site visit");
        body.put("actionDescription", "");
        mockMvc.perform(post("/grievance/createGrievanceAction/g1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("actionDescription is required")));
        verifyNoInteractions(fieldOfficerGrievanceService);
    }

    // --- updateGrievanceAction ---

    @Test
    @WithMockUser(username = "fo-1", roles = "FO")
    void updateAction_blankStatus_returns400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("grievanceActionTitle", "Site visit");
        body.put("actionDescription", "Fixed it");
        body.put("status", "");
        mockMvc.perform(put("/grievance/updateGrievanceAction/a1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("status is required")));
        verifyNoInteractions(fieldOfficerGrievanceService);
    }
}
