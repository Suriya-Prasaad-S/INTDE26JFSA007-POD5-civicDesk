package com.civicdesk.module.citizen.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
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

import com.civicdesk.module.citizen.service.DocumentService;
import com.civicdesk.module.citizen.support.FileStorageService;
import com.civicdesk.module.iam.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Bean-validation coverage for {@code PUT /citizenProfile/{citizenId}/verifyDocument/{documentId}}:
 * a blank/missing {@code status} must yield 400 and never reach the service.
 */
@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
class DocumentRequestValidationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DocumentService documentService;
    @MockitoBean
    private FileStorageService fileStorage;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    private void expectVerifyBadRequest(Map<String, Object> body, String messageFragment) throws Exception {
        mockMvc.perform(put("/citizenProfile/cit-1/verifyDocument/50000001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(messageFragment)));
        verifyNoInteractions(documentService);
    }

    @Test
    @WithMockUser(username = "officer-1", roles = "DS")
    void verifyDocument_blankStatus_returns400() throws Exception {
        expectVerifyBadRequest(Map.of("status", ""), "status is required");
    }

    @Test
    @WithMockUser(username = "officer-1", roles = "DS")
    void verifyDocument_whitespaceStatus_returns400() throws Exception {
        expectVerifyBadRequest(Map.of("status", "   "), "status is required");
    }

    @Test
    @WithMockUser(username = "officer-1", roles = "DS")
    void verifyDocument_missingStatusField_returns400() throws Exception {
        expectVerifyBadRequest(new HashMap<>(), "status is required");
    }

    @Test
    @WithMockUser(username = "officer-1", roles = "DS")
    void verifyDocument_malformedJson_returns400() throws Exception {
        mockMvc.perform(put("/citizenProfile/cit-1/verifyDocument/50000001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not-json "))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(documentService);
    }
}
