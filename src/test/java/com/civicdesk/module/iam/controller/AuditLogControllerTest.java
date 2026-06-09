package com.civicdesk.module.iam.controller;

import com.civicdesk.common.response.PageResponse;
import com.civicdesk.module.iam.security.JwtAuthFilter;
import com.civicdesk.module.iam.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditLogController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @WithMockUser(username = "admin-id", roles = "ADM")
    void admin_canReadAuditLogs_returns200() throws Exception {
        when(auditService.getAll(anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 0, 0));
        mockMvc.perform(get("/auditLogs")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "compliance-id", roles = "CO")
    void complianceOfficer_canReadAuditLogs_returns200() throws Exception {
        when(auditService.getAll(anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 0, 0));
        mockMvc.perform(get("/auditLogs")).andExpect(status().isOk());
    }

    // NOTE: role denial (403) for this endpoint is verified end-to-end in RbacIntegrationTest;
    // @WebMvcTest slices do not reliably enforce method security, so it isn't asserted here.
}
