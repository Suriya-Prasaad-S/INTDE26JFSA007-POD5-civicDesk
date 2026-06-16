package com.civicdesk.module.grievance.controller;

import com.civicdesk.module.grievance.dto.request.AssignFieldOfficerReq;
import com.civicdesk.module.grievance.dto.request.ResolveReq;
import com.civicdesk.module.grievance.dto.response.GrievanceSummaryResponse;
import com.civicdesk.module.grievance.enums.Category;
import com.civicdesk.module.grievance.enums.EscalationLevel;
import com.civicdesk.module.grievance.enums.GrievanceStatus;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Happy-path web-layer coverage for the supervisor grievance endpoints. RBAC denial (403)
 * is verified end-to-end in {@link com.civicdesk.module.grievance.integration.GrievanceFlowIntegrationTest}.
 */
@WebMvcTest(SupervisorGrievanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class SupervisorGrievanceControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SupervisorGrievanceService supervisorGrievanceService;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @WithMockUser(username = "sup-1", roles = "DS")
    void getDepartmentGrievances_returns200_withData() throws Exception {
        when(supervisorGrievanceService.getDepartmentGrievances())
                .thenReturn(List.of(summary(GrievanceStatus.O, EscalationLevel.L2)));

        mockMvc.perform(get("/grievance/getDepartmentGrievances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].grievanceId").value("g1"));
    }

    @Test
    @WithMockUser(username = "sup-1", roles = "DS")
    void assignFieldOfficer_returns200_withMessage() throws Exception {
        when(supervisorGrievanceService.assignFieldOfficer(eq("g1"), any(AssignFieldOfficerReq.class)))
                .thenReturn(summary(GrievanceStatus.IP, EscalationLevel.L1));

        mockMvc.perform(post("/grievance/assignFieldOfficer/g1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("fieldOfficerId", "fo-1", "message", "Please inspect"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Field officer assigned successfully"))
                .andExpect(jsonPath("$.data.status").value("IP"))
                .andExpect(jsonPath("$.data.escalationLevel").value("L1"));
    }

    @Test
    @WithMockUser(username = "sup-1", roles = "DS")
    void resolveGrievance_returns200_withMessage() throws Exception {
        when(supervisorGrievanceService.resolveGrievance(eq("g1"), any(ResolveReq.class)))
                .thenReturn(summary(GrievanceStatus.R, EscalationLevel.L2));

        mockMvc.perform(post("/grievance/resolveGrievance/g1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "Verified and fixed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Grievance resolved successfully"))
                .andExpect(jsonPath("$.data.status").value("R"));
    }

    private GrievanceSummaryResponse summary(GrievanceStatus status, EscalationLevel level) {
        return GrievanceSummaryResponse.builder()
                .grievanceId("g1")
                .grievanceTitle("Pothole")
                .category(Category.RI)
                .status(status)
                .escalationLevel(level)
                .build();
    }
}
