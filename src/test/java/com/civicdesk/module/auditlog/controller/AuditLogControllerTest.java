package com.civicdesk.module.auditlog.controller;

import com.civicdesk.common.response.PageResponse;
import com.civicdesk.module.auditlog.dto.response.AuditLogResponse;
import com.civicdesk.module.auditlog.service.AuditService;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditLogController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuditService auditService;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    private Map<String, Object> validBody() {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", "10000001");
        m.put("action", "LOGIN");
        m.put("module", "IAM");
        return m;
    }

    private AuditLogResponse sampleResponse() {
        return new AuditLogResponse("10000010", "10000001", "LOGIN", "IAM", "127.0.0.1", LocalDateTime.now());
    }

    private void expectPostBadRequest(Map<String, Object> body, String messageFragment) throws Exception {
        mockMvc.perform(post("/audit/auditLogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(messageFragment)));
        verify(auditService, never()).create(any(), any(), any(), any());
    }

    // ---- POST happy path ----------------------------------------------------

    @Test
    @WithMockUser(username = "10000001", roles = "CIT")
    void post_validBody_returns201() throws Exception {
        when(auditService.create(any(), any(), any(), any())).thenReturn(sampleResponse());
        mockMvc.perform(post("/audit/auditLogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "10000001", roles = "CIT")
    void post_validBody_returnsMessageAndData() throws Exception {
        when(auditService.create(any(), any(), any(), any())).thenReturn(sampleResponse());
        mockMvc.perform(post("/audit/auditLogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(jsonPath("$.message", is("Audit log recorded successfully")))
                .andExpect(jsonPath("$.data.auditId", is("10000010")))
                .andExpect(jsonPath("$.data.action", is("LOGIN")));
    }

    @Test
    @WithMockUser(username = "10000001", roles = "ADM")
    void post_passesBodyFieldsToService() throws Exception {
        when(auditService.create(any(), any(), any(), any())).thenReturn(sampleResponse());
        mockMvc.perform(post("/audit/auditLogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isCreated());
        verify(auditService).create(eq("10000001"), eq("LOGIN"), eq("IAM"), any());
    }

    @Test
    @WithMockUser(username = "10000001", roles = "CIT")
    void post_resolvesClientIpFromXForwardedFor() throws Exception {
        when(auditService.create(any(), any(), any(), any())).thenReturn(sampleResponse());
        mockMvc.perform(post("/audit/auditLogs")
                        .header("X-Forwarded-For", "203.0.113.9, 10.0.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isCreated());
        verify(auditService).create(eq("10000001"), eq("LOGIN"), eq("IAM"), eq("203.0.113.9"));
    }

    @Test
    @WithMockUser(username = "10000001", roles = "CIT")
    void post_lowercaseActionAndModule_accepted() throws Exception {
        when(auditService.create(any(), any(), any(), any())).thenReturn(sampleResponse());
        Map<String, Object> body = validBody();
        body.put("action", "login");
        body.put("module", "iam");
        mockMvc.perform(post("/audit/auditLogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "10000001", roles = "CIT")
    void post_maxLengthUserId_accepted() throws Exception {
        when(auditService.create(any(), any(), any(), any())).thenReturn(sampleResponse());
        Map<String, Object> body = validBody();
        body.put("userId", "12345678901234567890"); // 20 chars
        mockMvc.perform(post("/audit/auditLogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    // ---- POST validation ----------------------------------------------------

    @Test
    @WithMockUser(username = "10000001", roles = "CIT")
    void post_missingUserId_returns400() throws Exception {
        Map<String, Object> body = validBody();
        body.remove("userId");
        expectPostBadRequest(body, "userId is required");
    }

    @Test
    @WithMockUser(username = "10000001", roles = "CIT")
    void post_blankUserId_returns400() throws Exception {
        Map<String, Object> body = validBody();
        body.put("userId", "");
        expectPostBadRequest(body, "userId is required");
    }

    @Test
    @WithMockUser(username = "10000001", roles = "CIT")
    void post_userIdTooLong_returns400() throws Exception {
        Map<String, Object> body = validBody();
        body.put("userId", "123456789012345678901"); // 21 chars
        expectPostBadRequest(body, "userId must not exceed 20 characters");
    }

    @Test
    @WithMockUser(username = "10000001", roles = "CIT")
    void post_missingAction_returns400() throws Exception {
        Map<String, Object> body = validBody();
        body.remove("action");
        expectPostBadRequest(body, "action is required");
    }

    @Test
    @WithMockUser(username = "10000001", roles = "CIT")
    void post_invalidAction_returns400() throws Exception {
        Map<String, Object> body = validBody();
        body.put("action", "WIZARDRY");
        expectPostBadRequest(body, "action must be a valid audit action");
    }

    @Test
    @WithMockUser(username = "10000001", roles = "CIT")
    void post_missingModule_returns400() throws Exception {
        Map<String, Object> body = validBody();
        body.remove("module");
        expectPostBadRequest(body, "module is required");
    }

    @Test
    @WithMockUser(username = "10000001", roles = "CIT")
    void post_invalidModule_returns400() throws Exception {
        Map<String, Object> body = validBody();
        body.put("module", "ATLANTIS");
        expectPostBadRequest(body, "module must be a valid audit module");
    }

    @Test
    @WithMockUser(username = "10000001", roles = "CIT")
    void post_invalidBody_doesNotReachService() throws Exception {
        Map<String, Object> body = validBody();
        body.put("action", "NOPE");
        body.put("module", "NOPE");
        mockMvc.perform(post("/audit/auditLogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        verify(auditService, never()).create(any(), any(), any(), any());
    }

    // ---- GET list -----------------------------------------------------------

    @Test
    @WithMockUser(username = "admin-id", roles = "ADM")
    void admin_canReadAuditLogs_returns200() throws Exception {
        when(auditService.getAll(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 0, 0));
        mockMvc.perform(get("/audit/auditLogs")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "compliance-id", roles = "CO")
    void complianceOfficer_canReadAuditLogs_returns200() throws Exception {
        when(auditService.getAll(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 0, 0));
        mockMvc.perform(get("/audit/auditLogs")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin-id", roles = "ADM")
    void getAll_filterParamsArePassedToService() throws Exception {
        when(auditService.getAll(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 0, 0));

        mockMvc.perform(get("/audit/auditLogs")
                        .param("userId", "10000001")
                        .param("action", "LOGIN")
                        .param("module", "IAM")
                        .param("page", "2")
                        .param("size", "50"))
                .andExpect(status().isOk());

        verify(auditService).getAll(eq("10000001"), eq("LOGIN"), eq("IAM"), eq(2), eq(50));
    }

    @Test
    @WithMockUser(username = "admin-id", roles = "ADM")
    void getAll_defaultPagingApplied() throws Exception {
        when(auditService.getAll(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 0, 0));
        mockMvc.perform(get("/audit/auditLogs")).andExpect(status().isOk());
        verify(auditService).getAll(eq(null), eq(null), eq(null), eq(0), eq(20));
    }

    // ---- GET by id ----------------------------------------------------------

    @Test
    @WithMockUser(username = "admin-id", roles = "ADM")
    void getById_returns200WithData() throws Exception {
        when(auditService.getById("10000010")).thenReturn(sampleResponse());
        mockMvc.perform(get("/audit/auditLogs/10000010"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.auditId", is("10000010")));
    }

    @Test
    @WithMockUser(username = "admin-id", roles = "ADM")
    void getById_passesAuditIdToService() throws Exception {
        when(auditService.getById("10000010")).thenReturn(sampleResponse());
        mockMvc.perform(get("/audit/auditLogs/10000010")).andExpect(status().isOk());
        verify(auditService).getById("10000010");
    }
}
