package com.civicdesk.module.citizen.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import com.civicdesk.common.util.JwtUtil;
import com.civicdesk.module.iam.entity.User;
import com.civicdesk.module.iam.enums.Role;
import com.civicdesk.module.iam.enums.UserStatus;
import com.civicdesk.module.iam.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

/**
 * Full-stack citizen lifecycle over the real configured (test) DB and security stack: the citizen
 * profile journey (stub → verify → complete → read), the document journey (upload → list → get →
 * officer verify), and RBAC enforcement (401/403). Tokens are minted via {@link JwtUtil}; the
 * citizen and officer are seeded into the IAM {@code users} table. Document uploads are written to
 * a throwaway directory configured below.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties =
        "citizen.document.storage-dir=${java.io.tmpdir}/civicdesk-citizen-it-uploads")
@Transactional // roll back all DB writes so this test never pollutes the shared in-memory DB
class CitizenFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper om;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;

    private String citizenId;
    private String citizenToken;
    private String officerToken;

    @BeforeEach
    void seed() {
        User citizen = saveUser(Role.CIT.name());
        citizenId = citizen.getUserId();
        citizenToken = jwtUtil.generateToken(citizenId, Role.CIT.name());

        User officer = saveUser(Role.FO.name());
        officerToken = jwtUtil.generateToken(officer.getUserId(), Role.FO.name());
    }

    private User saveUser(String role) {
        User u = new User();
        u.setName(role + " user");
        u.setEmail(role.toLowerCase() + "." + UUID.randomUUID() + "@civicdesk.gov");
        u.setRole(role);
        u.setStatus(UserStatus.ACT.getLabel());
        u.setPasswordHash("x");
        u.setPasswordSet(true);
        return userRepository.save(u);
    }

    // ---------------------------------------------------------------------------------------------
    // Profile lifecycle
    // ---------------------------------------------------------------------------------------------

    @Test
    void profileLifecycle_stub_verify_complete_read() throws Exception {
        // 1) First GET /me lazily creates an Active stub.
        mockMvc.perform(get("/citizenProfile/me")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(citizenId))
                .andExpect(jsonPath("$.data.status").value("A"));

        // 2) The citizen now appears in the officer's pending-verification queue.
        mockMvc.perform(get("/citizenProfile/pendingVerifications")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk());

        // 3) Officer verifies the citizen (A -> V).
        mockMvc.perform(put("/citizenProfile/" + citizenId + "/verify")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("status", "V"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Citizen verification updated successfully"));

        // 4) The verified citizen completes their profile.
        mockMvc.perform(post("/citizenProfile/me")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "dateOfBirth", "1990-01-01",
                                "gender", "Male",
                                "nationalIdNumber", "IND1234567890",
                                "address", "12 Main St",
                                "ward", "Ward 12",
                                "zone", "Zone A"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile completed successfully"));

        // 5) Reading the profile back reflects the verified status and the masked national id.
        mockMvc.perform(get("/citizenProfile/me")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("V"))
                .andExpect(jsonPath("$.data.ward").value("Ward 12"))
                .andExpect(jsonPath("$.data.nationalIdNumber").value("****7890"));

        // 6) Officer ward listing now includes the citizen.
        mockMvc.perform(get("/citizenProfile/getCitizensByWard/Ward 12")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ward").value("Ward 12"));
    }

    @Test
    void completeBeforeVerification_returns409() throws Exception {
        // Create the Active stub, then attempt to complete before any verification.
        mockMvc.perform(get("/citizenProfile/me")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/citizenProfile/me")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "dateOfBirth", "1990-01-01",
                                "gender", "Male",
                                "nationalIdNumber", "IND1234567890",
                                "address", "12 Main St",
                                "ward", "Ward 12"))))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------------------------------------
    // Document lifecycle
    // ---------------------------------------------------------------------------------------------

    @Test
    void documentLifecycle_upload_list_get_verify() throws Exception {
        // The citizen profile must exist before uploading — created lazily by GET /me.
        mockMvc.perform(get("/citizenProfile/me")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk());

        MockMultipartFile file = new MockMultipartFile(
                "file", "national-id.pdf", "application/pdf", "pdf-bytes".getBytes());

        String uploadBody = mockMvc.perform(multipart("/citizenProfile/" + citizenId + "/uploadDocument")
                        .file(file)
                        .param("documentType", "NationalID")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Document uploaded successfully"))
                .andReturn().getResponse().getContentAsString();
        String documentId = JsonPath.read(uploadBody, "$.data");

        // List shows the uploaded document as Valid.
        mockMvc.perform(get("/citizenProfile/" + citizenId + "/getAllDocuments")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].documentId").value(documentId))
                .andExpect(jsonPath("$.data[0].status").value("V"));

        // Fetch by id, scoped to the owning citizen.
        mockMvc.perform(get("/citizenProfile/" + citizenId + "/getDocumentById/" + documentId)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").value(documentId));

        // Officer revokes the document (V -> R).
        mockMvc.perform(put("/citizenProfile/" + citizenId + "/verifyDocument/" + documentId)
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("status", "R"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Document verified successfully"));

        mockMvc.perform(get("/citizenProfile/" + citizenId + "/getDocumentById/" + documentId)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("R"));
    }

    @Test
    void citizen_cannotAccessAnothersDocuments_returns403() throws Exception {
        mockMvc.perform(get("/citizenProfile/me")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk());

        String otherCitizen = jwtUtil.generateToken(UUID.randomUUID().toString(), Role.CIT.name());
        mockMvc.perform(get("/citizenProfile/" + citizenId + "/getAllDocuments")
                        .header("Authorization", "Bearer " + otherCitizen))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------------------------------------
    // RBAC
    // ---------------------------------------------------------------------------------------------

    @Test
    void citizen_cannotHitOfficerEndpoint_returns403() throws Exception {
        mockMvc.perform(get("/citizenProfile/getAllCitizens")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void officer_cannotCompleteCitizenProfile_returns403() throws Exception {
        mockMvc.perform(post("/citizenProfile/me")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "dateOfBirth", "1990-01-01",
                                "gender", "Male",
                                "nationalIdNumber", "IND1234567890",
                                "address", "12 Main St",
                                "ward", "Ward 12"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/citizenProfile/me")).andExpect(status().isUnauthorized());
    }
}
