package com.civicdesk.module.citizen.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.civicdesk.module.citizen.dto.request.VerifyCitizenRequest;
import com.civicdesk.module.citizen.dto.response.CitizenProfileResponse;
import com.civicdesk.module.citizen.dto.response.CitizenSummaryResponse;
import com.civicdesk.module.citizen.service.CitizenService;
import com.civicdesk.module.citizen.support.FileStorageService;
import com.civicdesk.module.iam.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Happy-path web-layer coverage for the citizen profile endpoints: correct HTTP status and the
 * {@code $.message}/{@code $.data} envelope. RBAC denial is exercised end-to-end in
 * {@link com.civicdesk.module.citizen.integration.CitizenFlowIntegrationTest};
 * {@code @WebMvcTest} slices disable filters, mirroring the grievance/IAM controller tests.
 */
@WebMvcTest(CitizenController.class)
@AutoConfigureMockMvc(addFilters = false)
class CitizenControllerTest {

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

    @Test
    void register_returns201_withMessage() throws Exception {
        MockMultipartFile proof = new MockMultipartFile(
                "proof", "proof.pdf", "application/pdf", "proof-bytes".getBytes());

        mockMvc.perform(multipart("/citizenProfile/register")
                        .file(proof)
                        .param("name", "Ravi Kumar")
                        .param("email", "ravi@example.com")
                        .param("password", "Ravi@1234")
                        .param("phone", "9876543210")
                        .param("dateOfBirth", "1990-01-01")
                        .param("gender", "Male")
                        .param("nationalIdNumber", "IND1234567890")
                        .param("address", "12 Main St")
                        .param("ward", "Ward 12")
                        .param("zone", "Zone A"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Citizen registered successfully"));

        verify(citizenService).registerCitizen(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void getMyProfile_returns200_withData() throws Exception {
        when(citizenService.getMyProfile()).thenReturn(profileResponse());

        mockMvc.perform(get("/citizenProfile/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("cit-1"))
                .andExpect(jsonPath("$.data.status").value("V"))
                .andExpect(jsonPath("$.data.nationalIdNumber").value("****7890"));
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void updateMyProfile_returns200_withMessage() throws Exception {
        mockMvc.perform(put("/citizenProfile/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("address", "99 New Road"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile updated successfully"));

        verify(citizenService).updateMyProfile(any());
    }

    @Test
    @WithMockUser(username = "officer-1", roles = "FO")
    void getPendingVerifications_returns200_withDataArray() throws Exception {
        when(citizenService.getPendingVerifications())
                .thenReturn(List.of(new CitizenSummaryResponse("c1", "Alice", "Ward 12", "A")));

        mockMvc.perform(get("/citizenProfile/pendingVerifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value("c1"))
                .andExpect(jsonPath("$.data[0].status").value("A"));
    }

    @Test
    @WithMockUser(username = "officer-1", roles = "FO")
    void verifyCitizen_returns200_withMessage() throws Exception {
        mockMvc.perform(put("/citizenProfile/c1/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyCitizenRequest("V"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Citizen verification updated successfully"));

        verify(citizenService).verifyCitizen(eq("c1"), any(VerifyCitizenRequest.class));
    }

    @Test
    @WithMockUser(username = "officer-1", roles = "DS")
    void getCitizensByWard_returns200_withData() throws Exception {
        when(citizenService.getCitizensByWard("Ward 12"))
                .thenReturn(List.of(new CitizenSummaryResponse("c1", "Alice", "Ward 12", "V")));

        mockMvc.perform(get("/citizenProfile/getCitizensByWard/Ward 12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ward").value("Ward 12"));
    }

    @Test
    @WithMockUser(username = "officer-1", roles = "ADM")
    void getAllCitizens_returns200_withData() throws Exception {
        when(citizenService.getAllCitizens())
                .thenReturn(List.of(new CitizenSummaryResponse("c1", "Alice", "Ward 12", "V")));

        mockMvc.perform(get("/citizenProfile/getAllCitizens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value("c1"));
    }

    @Test
    @WithMockUser(username = "officer-1", roles = "DS")
    void getProof_streamsFile() throws Exception {
        Resource resource = new ByteArrayResource("proof-bytes".getBytes());
        when(citizenService.resolveProofFileName("cit-1")).thenReturn("proof.pdf");
        when(fileStorage.load("proof.pdf")).thenReturn(resource);

        mockMvc.perform(get("/citizenProfile/cit-1/proof"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    private CitizenProfileResponse profileResponse() {
        return new CitizenProfileResponse(
                "cit-1", "Ravi Kumar", "ravi@example.com", "9000000000",
                LocalDate.of(1990, 1, 1), "Male", "****7890", "12 Main St", "Ward 12", "Zone A",
                "V", "officer-1", LocalDateTime.now(), LocalDateTime.now());
    }
}
