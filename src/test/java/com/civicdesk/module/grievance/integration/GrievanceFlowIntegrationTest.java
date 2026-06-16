package com.civicdesk.module.grievance.integration;

import com.civicdesk.common.util.JwtUtil;
import com.civicdesk.module.iam.entity.Department;
import com.civicdesk.module.iam.entity.User;
import com.civicdesk.module.iam.enums.Role;
import com.civicdesk.module.iam.enums.UserStatus;
import com.civicdesk.module.iam.repository.DepartmentRepository;
import com.civicdesk.module.iam.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack grievance lifecycle over the real configured (test) DB and security stack.
 * Tokens are minted directly via {@link JwtUtil} (the grievance tables have no FK to users,
 * so a citizen needs no row); the DS/FO users and the supervisor link are seeded into the
 * IAM tables the same way the IAM integration tests rely on seeded data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // roll back all DB writes so this test never pollutes the shared in-memory DB
class GrievanceFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper om;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DepartmentRepository departmentRepository;

    private String citizenToken;
    private String supervisorToken;
    private String fieldOfficerToken;
    private String fieldOfficerId;

    @BeforeEach
    void seed() {
        // Infrastructure is the department that category RI routes to (seeded by DataSeeder).
        Department dept = departmentRepository.findByName("Infrastructure").orElseThrow();

        // A supervisor for the department, with a unique-per-run identity.
        User supervisor = saveUser(Role.DS.name(), dept.getDepartmentId());
        dept.setDepartmentSupervisorId(supervisor.getUserId());
        departmentRepository.save(dept);

        // An active field officer in the same department.
        User fieldOfficer = saveUser(Role.FO.name(), dept.getDepartmentId());
        fieldOfficerId = fieldOfficer.getUserId();

        // The citizen has no DB row — only a JWT identity.
        citizenToken = jwtUtil.generateToken(UUID.randomUUID().toString(), Role.CIT.name());
        supervisorToken = jwtUtil.generateToken(supervisor.getUserId(), Role.DS.name());
        fieldOfficerToken = jwtUtil.generateToken(fieldOfficer.getUserId(), Role.FO.name());
    }

    private User saveUser(String role, String departmentId) {
        User u = new User();
        u.setName(role + " user");
        u.setEmail(role.toLowerCase() + "." + UUID.randomUUID() + "@civicdesk.gov");
        u.setRole(role);
        u.setDepartmentId(departmentId);
        u.setStatus(UserStatus.ACT.getLabel());
        u.setPasswordHash("x");
        u.setPasswordSet(true);
        return userRepository.save(u);
    }

    @Test
    void fullLifecycle_create_assign_work_complete_resolve_close() throws Exception {
        // 1) Citizen creates a grievance -> status O, escalation L2.
        String createBody = mockMvc.perform(post("/grievance/createGrievance")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "category", "RI",
                                "grievanceTitle", "Pothole on main road",
                                "description", "Deep pothole near the bus stop",
                                "ward", "Ward 12"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Grievance created successfully"))
                .andExpect(jsonPath("$.data.status").value("O"))
                .andExpect(jsonPath("$.data.escalationLevel").value("L2"))
                .andReturn().getResponse().getContentAsString();
        String grievanceId = JsonPath.read(createBody, "$.data.grievanceId");

        // 2) Supervisor assigns the field officer -> status IP, escalation L1.
        mockMvc.perform(post("/grievance/assignFieldOfficer/" + grievanceId)
                        .header("Authorization", "Bearer " + supervisorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "fieldOfficerId", fieldOfficerId,
                                "message", "Please inspect and fix"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IP"))
                .andExpect(jsonPath("$.data.escalationLevel").value("L1"));

        // 3) Field officer creates a WORK action (Open).
        String actionBody = mockMvc.perform(post("/grievance/createGrievanceAction/" + grievanceId)
                        .header("Authorization", "Bearer " + fieldOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "grievanceActionTitle", "Site visit",
                                "actionDescription", "Inspected the location"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actionType").value("WK"))
                .andExpect(jsonPath("$.data.status").value("O"))
                .andReturn().getResponse().getContentAsString();
        String actionId = JsonPath.read(actionBody, "$.data.actionId");

        // 4) Field officer marks the WORK action Completed -> handoff back to supervisor (L2).
        mockMvc.perform(put("/grievance/updateGrievanceAction/" + actionId)
                        .header("Authorization", "Bearer " + fieldOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("status", "CM"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CM"));

        // 5) Supervisor resolves -> status R.
        mockMvc.perform(post("/grievance/resolveGrievance/" + grievanceId)
                        .header("Authorization", "Bearer " + supervisorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("message", "Verified and fixed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("R"));

        // 6) Citizen closes -> status C.
        mockMvc.perform(post("/grievance/closeGrievance/" + grievanceId)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("C"));

        // The timeline accrued the expected action types (oldest first): AS, WK, RV, RS, CL.
        mockMvc.perform(get("/grievance/getGrievanceById/" + grievanceId)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grievance.status").value("C"))
                .andExpect(jsonPath("$.data.actions[0].actionType").value("AS"))
                .andExpect(jsonPath("$.data.actions[1].actionType").value("WK"))
                .andExpect(jsonPath("$.data.actions[2].actionType").value("RV"))
                .andExpect(jsonPath("$.data.actions[3].actionType").value("RS"))
                .andExpect(jsonPath("$.data.actions[4].actionType").value("CL"));
    }

    @Test
    void citizen_cannotHitSupervisorEndpoint_returns403() throws Exception {
        mockMvc.perform(get("/grievance/getDepartmentGrievances")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void supervisor_cannotHitCitizenCreate_returns403() throws Exception {
        mockMvc.perform(post("/grievance/createGrievance")
                        .header("Authorization", "Bearer " + supervisorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "category", "RI",
                                "grievanceTitle", "x",
                                "description", "y"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/grievance/getMyGrievances")).andExpect(status().isUnauthorized());
    }

    @Test
    void citizen_closeWhenNotResolved_returns409() throws Exception {
        // Fresh grievance is Open, not Resolved -> closing must conflict.
        String createBody = mockMvc.perform(post("/grievance/createGrievance")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "category", "RI",
                                "grievanceTitle", "Broken streetlight",
                                "description", "Light out for a week"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String grievanceId = JsonPath.read(createBody, "$.data.grievanceId");

        mockMvc.perform(post("/grievance/closeGrievance/" + grievanceId)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isConflict());
    }

    @Test
    void citizen_cannotAccessAnothersGrievance_returns403() throws Exception {
        String createBody = mockMvc.perform(post("/grievance/createGrievance")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "category", "RI",
                                "grievanceTitle", "Drain overflow",
                                "description", "Blocked drain on 5th cross"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String grievanceId = JsonPath.read(createBody, "$.data.grievanceId");

        String otherCitizen = jwtUtil.generateToken(UUID.randomUUID().toString(), Role.CIT.name());
        mockMvc.perform(get("/grievance/getGrievanceById/" + grievanceId)
                        .header("Authorization", "Bearer " + otherCitizen))
                .andExpect(status().isForbidden());
    }
}
