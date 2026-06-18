package com.civicdesk.module.citizen.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.civicdesk.common.util.JwtUtil;
import com.civicdesk.module.citizen.service.DocumentService;
import com.civicdesk.module.iam.entity.User;
import com.civicdesk.module.iam.enums.Role;
import com.civicdesk.module.iam.enums.UserStatus;
import com.civicdesk.module.iam.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Full-stack citizen lifecycle over the real configured (test) DB and security stack: registration
 * (User + CitizenProfile created together), officer verification, profile read, the document wallet
 * (pushed in-process by the service-request side via {@link DocumentService#addDocument}), proof
 * access, and RBAC (401/403). Document/proof files are written to a throwaway directory.
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
    @Autowired
    private DocumentService documentService;

    private String officerToken;

    @BeforeEach
    void seed() {
        User officer = saveUser(Role.FO.name());
        officerToken = jwtUtil.generateToken(officer.getUserId(), Role.FO.name());
    }

    /** Seeds a staff user directly (officers aren't created via the citizen flow). */
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

    /** Registers a citizen via the public multipart endpoint; returns the new userId. */
    private String registerCitizen(String email) throws Exception {
        MockMultipartHttpServletRequestBuilder b = multipart("/citizenProfile/register")
                .file(new MockMultipartFile("proof", "proof.pdf", "application/pdf", "proof".getBytes()));
        b.param("name", "Ravi Kumar").param("email", email).param("password", "Ravi@1234")
                .param("phone", "9876543210").param("dateOfBirth", "1990-01-01").param("gender", "Male")
                .param("nationalIdNumber", "IND1234567890").param("address", "12 Main St")
                .param("ward", "Ward 12").param("zone", "Zone A");
        mockMvc.perform(b)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Citizen registered successfully"));
        return userRepository.findByEmail(email).orElseThrow().getUserId();
    }

    private String citizenEmail() {
        return "citizen." + UUID.randomUUID() + "@example.com";
    }

    // ---------------------------------------------------------------------------------------------
    // Registration → verify → read
    // ---------------------------------------------------------------------------------------------

    @Test
    void registration_verify_read() throws Exception {
        String userId = registerCitizen(citizenEmail());
        String citizenToken = jwtUtil.generateToken(userId, Role.CIT.name());

        // Fresh registration: status Active, profile populated, national id masked.
        mockMvc.perform(get("/citizenProfile/me")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.status").value("A"))
                .andExpect(jsonPath("$.data.ward").value("Ward 12"))
                .andExpect(jsonPath("$.data.nationalIdNumber").value("****7890"));

        // Appears in the officer's pending-verification queue.
        mockMvc.perform(get("/citizenProfile/pendingVerifications")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk());

        // Officer reviews the proof, then verifies (A -> V).
        mockMvc.perform(get("/citizenProfile/" + userId + "/proof")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/citizenProfile/" + userId + "/verify")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("status", "V"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/citizenProfile/me")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("V"));

        mockMvc.perform(get("/citizenProfile/getCitizensByWard/Ward 12")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ward").value("Ward 12"));
    }

    @Test
    void duplicateNationalId_secondRegistration_returns409() throws Exception {
        registerCitizen(citizenEmail());

        // Same national id, different email -> conflict.
        MockMultipartHttpServletRequestBuilder b = multipart("/citizenProfile/register")
                .file(new MockMultipartFile("proof", "proof.pdf", "application/pdf", "proof".getBytes()));
        b.param("name", "Other").param("email", citizenEmail()).param("password", "Pass@1234")
                .param("phone", "9876500000").param("dateOfBirth", "1992-02-02").param("gender", "Female")
                .param("nationalIdNumber", "IND1234567890").param("address", "9 Side St")
                .param("ward", "Ward 13");
        mockMvc.perform(b).andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------------------------------------
    // Document wallet (pushed in-process, read over HTTP)
    // ---------------------------------------------------------------------------------------------

    @Test
    void documentWallet_pushedThenListedAndDownloaded() throws Exception {
        String userId = registerCitizen(citizenEmail());
        String citizenToken = jwtUtil.generateToken(userId, Role.CIT.name());

        // Service-request side pushes a generated document into the wallet (in-process).
        String documentId = documentService.addDocument(
                userId, "BirthCertificate", "birth.pdf", "pdf-bytes".getBytes(), null, null);

        mockMvc.perform(get("/citizenProfile/" + userId + "/getAllDocuments")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].documentId").value(documentId))
                .andExpect(jsonPath("$.data[0].status").value("V"));

        mockMvc.perform(get("/citizenProfile/" + userId + "/getDocumentById/" + documentId)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").value(documentId));

        mockMvc.perform(get("/citizenProfile/" + userId + "/documents/" + documentId + "/file")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk());
    }

    @Test
    void citizen_cannotAccessAnothersDocuments_returns403() throws Exception {
        String userId = registerCitizen(citizenEmail());
        String otherCitizen = jwtUtil.generateToken(UUID.randomUUID().toString(), Role.CIT.name());

        mockMvc.perform(get("/citizenProfile/" + userId + "/getAllDocuments")
                        .header("Authorization", "Bearer " + otherCitizen))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------------------------------------
    // RBAC
    // ---------------------------------------------------------------------------------------------

    @Test
    void citizen_cannotHitOfficerEndpoint_returns403() throws Exception {
        String userId = registerCitizen(citizenEmail());
        String citizenToken = jwtUtil.generateToken(userId, Role.CIT.name());

        mockMvc.perform(get("/citizenProfile/getAllCitizens")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/citizenProfile/me")).andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------------------------------------
    // Verification gate (unverified citizens blocked from other modules)
    // ---------------------------------------------------------------------------------------------

    @Test
    void unverifiedCitizen_blockedFromGrievanceModule_returns403() throws Exception {
        String userId = registerCitizen(citizenEmail()); // status Active (not verified)
        String citizenToken = jwtUtil.generateToken(userId, Role.CIT.name());

        mockMvc.perform(get("/grievance/getMyGrievances")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void verifiedCitizen_passesGateOnGrievanceModule() throws Exception {
        String userId = registerCitizen(citizenEmail());
        String citizenToken = jwtUtil.generateToken(userId, Role.CIT.name());

        mockMvc.perform(put("/citizenProfile/" + userId + "/verify")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("status", "V"))))
                .andExpect(status().isOk());

        // Now verified, the gate lets the citizen reach the grievance module.
        mockMvc.perform(get("/grievance/getMyGrievances")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk());
    }

    @Test
    void unverifiedCitizen_canStillReachOwnProfile() throws Exception {
        String userId = registerCitizen(citizenEmail());
        String citizenToken = jwtUtil.generateToken(userId, Role.CIT.name());

        // /citizenProfile/** is exempt from the gate.
        mockMvc.perform(get("/citizenProfile/me")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk());
    }
}
