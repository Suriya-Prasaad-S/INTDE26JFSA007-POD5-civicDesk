package com.civicdesk.module.grievance.controller;

import com.civicdesk.module.grievance.dto.request.GrievanceActionCreateReq;
import com.civicdesk.module.grievance.dto.request.GrievanceActionUpdateReq;
import com.civicdesk.module.grievance.dto.response.GrievanceActionResponse;
import com.civicdesk.module.grievance.enums.ActionStatus;
import com.civicdesk.module.grievance.enums.ActionType;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Happy-path web-layer coverage for the field-officer grievance endpoints. RBAC denial (403)
 * is verified end-to-end in {@link com.civicdesk.module.grievance.integration.GrievanceFlowIntegrationTest}.
 */
@WebMvcTest(FieldOfficerGrievanceController.class)
@AutoConfigureMockMvc(addFilters = false)
class FieldOfficerGrievanceControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FieldOfficerGrievanceService fieldOfficerGrievanceService;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @WithMockUser(username = "fo-1", roles = "FO")
    void getAssignedGrievances_returns200_withData() throws Exception {
        when(fieldOfficerGrievanceService.getAssignedGrievances()).thenReturn(List.of());

        mockMvc.perform(get("/grievance/getAssignedGrievances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "fo-1", roles = "FO")
    void createGrievanceAction_returns200_withMessage() throws Exception {
        when(fieldOfficerGrievanceService.createGrievanceAction(eq("g1"), any(GrievanceActionCreateReq.class)))
                .thenReturn(action(ActionStatus.O));

        mockMvc.perform(post("/grievance/createGrievanceAction/g1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "grievanceActionTitle", "Site visit",
                                "actionDescription", "Inspected the location"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Work action created successfully"))
                .andExpect(jsonPath("$.data.actionType").value("WK"))
                .andExpect(jsonPath("$.data.status").value("O"));
    }

    @Test
    @WithMockUser(username = "fo-1", roles = "FO")
    void updateGrievanceAction_returns200_withMessage() throws Exception {
        when(fieldOfficerGrievanceService.updateGrievanceAction(eq("a1"), any(GrievanceActionUpdateReq.class)))
                .thenReturn(action(ActionStatus.CM));

        mockMvc.perform(put("/grievance/updateGrievanceAction/a1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CM"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Work action updated successfully"))
                .andExpect(jsonPath("$.data.status").value("CM"));
    }

    private GrievanceActionResponse action(ActionStatus status) {
        return GrievanceActionResponse.builder()
                .actionId("a1")
                .actionType(ActionType.WK)
                .grievanceActionTitle("Site visit")
                .actionDescription("Inspected the location")
                .status(status)
                .takenById("fo-1")
                .build();
    }
}
