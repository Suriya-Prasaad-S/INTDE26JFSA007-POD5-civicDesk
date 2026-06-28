package com.civicdesk.module.iam.controller;

import com.civicdesk.module.iam.dto.request.CreateUserRequest;
import com.civicdesk.module.iam.dto.response.UserResponse;
import com.civicdesk.module.iam.security.JwtAuthFilter;
import com.civicdesk.module.auditlog.service.AuditService;
import com.civicdesk.module.iam.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private AuditService auditService;
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin-id", roles = "ADM")
    void adminCreatesUser_returns201() throws Exception {
        when(userService.createUser(any(), anyString(), anyString()))
                .thenReturn(new UserResponse());

        CreateUserRequest req = new CreateUserRequest();
        req.setName("Meena");
        req.setEmail("meena@civicdesk.gov");
        req.setPhone("9876543210");
        req.setRole("DS");
        req.setDepartmentId("dept-1");

        mockMvc.perform(post("/iam/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    // NOTE: @PreAuthorize denial (403) is verified end-to-end in RbacIntegrationTest;
    // @WebMvcTest slices do not reliably enforce method security, so it isn't asserted here.

    @Test
    @WithMockUser(username = "admin-id", roles = "ADM")
    void adminListsUsers_returns200() throws Exception {
        when(userService.getUsers(anyString(), anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new com.civicdesk.common.response.PageResponse<>(java.util.List.of(), 0, 0, 0));

        mockMvc.perform(get("/iam/users"))
                .andExpect(status().isOk());
    }
}
