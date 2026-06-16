package com.civicdesk.module.grievance.controller;

import com.civicdesk.module.grievance.dto.request.GrievanceCreateReq;
import com.civicdesk.module.grievance.dto.request.GrievanceReopenReq;
import com.civicdesk.module.grievance.dto.response.GrievanceResponse;
import com.civicdesk.module.grievance.dto.response.GrievanceSummaryResponse;
import com.civicdesk.module.grievance.enums.Category;
import com.civicdesk.module.grievance.enums.EscalationLevel;
import com.civicdesk.module.grievance.enums.GrievanceStatus;
import com.civicdesk.module.grievance.service.CitizenGrievanceService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Happy-path web-layer coverage for the citizen grievance endpoints: correct HTTP status,
 * {@code $.message} and {@code $.data} envelope. RBAC denial (403) is verified end-to-end
 * in {@link com.civicdesk.module.grievance.integration.GrievanceFlowIntegrationTest};
 * @WebMvcTest slices do not reliably enforce method security, mirroring the IAM tests.
 */
@WebMvcTest(CitizenGrievanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class CitizenGrievanceControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CitizenGrievanceService citizenGrievanceService;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void createGrievance_returns201_withData() throws Exception {
        when(citizenGrievanceService.createGrievance(any())).thenReturn(grievanceResponse(GrievanceStatus.O));

        GrievanceCreateReq req = new GrievanceCreateReq();
        req.setCategory("RI");
        req.setGrievanceTitle("Pothole");
        req.setDescription("Deep pothole near the bus stop");
        req.setWard("Ward 12");

        mockMvc.perform(post("/grievance/createGrievance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Grievance created successfully"))
                .andExpect(jsonPath("$.data.grievanceId").value("g1"))
                .andExpect(jsonPath("$.data.status").value("O"));
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void getMyGrievances_returns200_withData() throws Exception {
        when(citizenGrievanceService.getMyGrievances()).thenReturn(List.of(summary()));

        mockMvc.perform(get("/grievance/getMyGrievances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].grievanceId").value("g1"));
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void closeGrievance_returns200_withMessage() throws Exception {
        when(citizenGrievanceService.closeGrievance(eq("g1"))).thenReturn(grievanceResponse(GrievanceStatus.C));

        mockMvc.perform(post("/grievance/closeGrievance/g1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Grievance closed successfully"))
                .andExpect(jsonPath("$.data.status").value("C"));
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void reopenGrievance_returns200_withMessage() throws Exception {
        when(citizenGrievanceService.reopenGrievance(eq("g1"), any(GrievanceReopenReq.class)))
                .thenReturn(grievanceResponse(GrievanceStatus.RO));

        mockMvc.perform(post("/grievance/reopenGrievance/g1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Not actually fixed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Grievance reopened successfully"))
                .andExpect(jsonPath("$.data.status").value("RO"));
    }

    @Test
    @WithMockUser(username = "cit-1", roles = "CIT")
    void getGrievanceById_returns200_withDetail() throws Exception {
        when(citizenGrievanceService.getGrievanceById(anyString())).thenReturn(
                com.civicdesk.module.grievance.dto.response.GrievanceDetailResponse.builder()
                        .grievance(grievanceResponse(GrievanceStatus.O))
                        .actions(List.of())
                        .build());

        mockMvc.perform(get("/grievance/getGrievanceById/g1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grievance.grievanceId").value("g1"));
    }

    private GrievanceResponse grievanceResponse(GrievanceStatus status) {
        return GrievanceResponse.builder()
                .grievanceId("g1")
                .category(Category.RI)
                .grievanceTitle("Pothole")
                .description("Deep pothole near the bus stop")
                .ward("Ward 12")
                .status(status)
                .escalationLevel(EscalationLevel.L2)
                .build();
    }

    private GrievanceSummaryResponse summary() {
        return GrievanceSummaryResponse.builder()
                .grievanceId("g1")
                .grievanceTitle("Pothole")
                .category(Category.RI)
                .status(GrievanceStatus.O)
                .escalationLevel(EscalationLevel.L2)
                .build();
    }
}
