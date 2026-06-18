package com.civicdesk.module.citizen.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.civicdesk.module.citizen.service.CitizenService;
import com.civicdesk.module.citizen.support.FileStorageService;
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
    private FileStorageService fileStorage;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    private Map<String, String> validRegister() {
        Map<String, String> m = new HashMap<>();
        m.put("name", "Ravi Kumar");
        m.put("email", "ravi@example.com");
        m.put("password", "Ravi@1234");
        m.put("phone", "9876543210");
        m.put("dateOfBirth", "1990-01-01");
        m.put("gender", "Male");
        m.put("nationalIdNumber", "IND1234567890");
        m.put("address", "12 Main St");
        m.put("ward", "Ward 12");
        m.put("zone", "Zone A");
        return m;
    }

    private void expectRegisterBadRequest(Map<String, String> params, String messageFragment) throws Exception {
        MockMultipartHttpServletRequestBuilder builder = multipart("/citizenProfile/register")
                .file(new MockMultipartFile("proof", "proof.pdf", "application/pdf", "b".getBytes()));
        params.forEach(builder::param);
        mockMvc.perform(builder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(messageFragment)));
        verifyNoInteractions(citizenService);
    }

    // --- register (POST /citizenProfile/register, multipart) ---

    @Test
    void register_blankName_returns400() throws Exception {
        Map<String, String> body = validRegister();
        body.put("name", "");
        expectRegisterBadRequest(body, "name is required");
    }

    @Test
    void register_malformedEmail_returns400() throws Exception {
        Map<String, String> body = validRegister();
        body.put("email", "nope");
        expectRegisterBadRequest(body, "email must be a valid email address");
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        Map<String, String> body = validRegister();
        body.put("password", "short");
        expectRegisterBadRequest(body, "password must be between 8 and 72 characters");
    }

    @Test
    void register_invalidPhone_returns400() throws Exception {
        Map<String, String> body = validRegister();
        body.put("phone", "12345");
        expectRegisterBadRequest(body, "phone must be a valid 10-digit Indian mobile number");
    }

    @Test
    void register_missingDateOfBirth_returns400() throws Exception {
        Map<String, String> body = validRegister();
        body.remove("dateOfBirth");
        expectRegisterBadRequest(body, "dateOfBirth is required");
    }

    @Test
    void register_futureDateOfBirth_returns400() throws Exception {
        Map<String, String> body = validRegister();
        body.put("dateOfBirth", "2999-01-01");
        expectRegisterBadRequest(body, "dateOfBirth must be a date in the past");
    }

    @Test
    void register_invalidGenderPattern_returns400() throws Exception {
        Map<String, String> body = validRegister();
        body.put("gender", "Martian");
        expectRegisterBadRequest(body, "gender must be Male, Female or Other");
    }

    @Test
    void register_invalidNationalIdPattern_returns400() throws Exception {
        Map<String, String> body = validRegister();
        body.put("nationalIdNumber", "ab$");
        expectRegisterBadRequest(body, "nationalIdNumber must be 6-20 alphanumeric characters");
    }

    @Test
    void register_blankAddress_returns400() throws Exception {
        Map<String, String> body = validRegister();
        body.put("address", "");
        expectRegisterBadRequest(body, "address is required");
    }

    @Test
    void register_blankWard_returns400() throws Exception {
        Map<String, String> body = validRegister();
        body.put("ward", "");
        expectRegisterBadRequest(body, "ward is required");
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
