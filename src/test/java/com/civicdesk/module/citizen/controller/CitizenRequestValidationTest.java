package com.civicdesk.module.citizen.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.civicdesk.module.citizen.service.CitizenService;
import com.civicdesk.module.iam.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Bean-validation coverage for the citizen profile endpoints: each invalid attribute must yield
 * 400 with a precise message and never reach the service.
 */
@WebMvcTest(CitizenController.class)
@AutoConfigureMockMvc(addFilters = false)
class CitizenRequestValidationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CitizenService citizenService;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    private Map<String, Object> validComplete() {
        Map<String, Object> m = new HashMap<>();
        m.put("dateOfBirth", "1990-01-01");
        m.put("gender", "Male");
        m.put("nationalIdNumber", "IND1234567890");
        m.put("address", "12 Main St");
        m.put("ward", "Ward 12");
        m.put("zone", "Zone A");
        return m;
    }

    private void expectCompleteBadRequest(Map<String, Object> body, String messageFragment) throws Exception {
        mockMvc.perform(post("/citizenProfile/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(messageFragment)));
        verifyNoInteractions(citizenService);
    }

    // --- completeProfile (POST /citizenProfile/me) ---

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void complete_missingDateOfBirth_returns400() throws Exception {
        Map<String, Object> body = validComplete();
        body.remove("dateOfBirth");
        expectCompleteBadRequest(body, "dateOfBirth is required");
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void complete_futureDateOfBirth_returns400() throws Exception {
        Map<String, Object> body = validComplete();
        body.put("dateOfBirth", "2999-01-01");
        expectCompleteBadRequest(body, "dateOfBirth must be a date in the past");
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void complete_blankGender_returns400() throws Exception {
        Map<String, Object> body = validComplete();
        body.put("gender", "");
        expectCompleteBadRequest(body, "gender is required");
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void complete_invalidGenderPattern_returns400() throws Exception {
        Map<String, Object> body = validComplete();
        body.put("gender", "Martian");
        expectCompleteBadRequest(body, "gender must be Male, Female or Other");
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void complete_blankNationalId_returns400() throws Exception {
        Map<String, Object> body = validComplete();
        body.put("nationalIdNumber", "");
        expectCompleteBadRequest(body, "nationalIdNumber is required");
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void complete_invalidNationalIdPattern_returns400() throws Exception {
        Map<String, Object> body = validComplete();
        body.put("nationalIdNumber", "ab$"); // too short + illegal char
        expectCompleteBadRequest(body, "nationalIdNumber must be 6-20 alphanumeric characters");
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void complete_blankAddress_returns400() throws Exception {
        Map<String, Object> body = validComplete();
        body.put("address", "");
        expectCompleteBadRequest(body, "address is required");
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void complete_oversizeAddress_returns400() throws Exception {
        Map<String, Object> body = validComplete();
        body.put("address", "x".repeat(256));
        expectCompleteBadRequest(body, "address must not exceed 255 characters");
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void complete_blankWard_returns400() throws Exception {
        Map<String, Object> body = validComplete();
        body.put("ward", "");
        expectCompleteBadRequest(body, "ward is required");
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void complete_oversizeZone_returns400() throws Exception {
        Map<String, Object> body = validComplete();
        body.put("zone", "x".repeat(51));
        expectCompleteBadRequest(body, "zone must not exceed 50 characters");
    }

    // --- updateMyProfile (PUT /citizenProfile/me) ---

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void update_oversizeAddress_returns400() throws Exception {
        mockMvc.perform(put("/citizenProfile/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("address", "x".repeat(256)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("address must not exceed 255 characters")));
        verifyNoInteractions(citizenService);
    }

    // --- verifyCitizen (PUT /citizenProfile/{userId}/verify) ---

    @Test
    @WithMockUser(username = "officer-1", roles = "FO")
    void verify_blankStatus_returns400() throws Exception {
        mockMvc.perform(put("/citizenProfile/c1/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("status is required")));
        verifyNoInteractions(citizenService);
    }
}
