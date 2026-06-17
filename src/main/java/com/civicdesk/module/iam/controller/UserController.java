package com.civicdesk.module.iam.controller;

import com.civicdesk.common.response.ApiResponse;
import com.civicdesk.common.response.PageResponse;
import com.civicdesk.common.util.ClientIpUtil;
import com.civicdesk.common.util.SecurityContextUtil;
import com.civicdesk.module.iam.dto.request.CreateUserRequest;
import com.civicdesk.module.iam.dto.request.UpdateUserStatusRequest;
import com.civicdesk.module.auditlog.enums.AuditAction;
import com.civicdesk.module.auditlog.enums.AuditModule;
import com.civicdesk.module.auditlog.service.AuditService;
import com.civicdesk.module.iam.dto.response.UserResponse;
import com.civicdesk.module.iam.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/iam/users")
public class UserController {

    private final UserService userService;
    private final AuditService auditService;

    public UserController(UserService userService, AuditService auditService) {
        this.userService = userService;
        this.auditService = auditService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getMe() {
        String userId = SecurityContextUtil.getCurrentUserId();
        UserResponse user = userService.getById(userId);
        return ResponseEntity.ok(ApiResponse.data(user));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADM', 'DS')")
    public ResponseEntity<ApiResponse> createUser(
            @Valid @RequestBody CreateUserRequest req,
            HttpServletRequest httpReq) {
        String callerRole = SecurityContextUtil.getCurrentRole();
        String callerUserId = SecurityContextUtil.getCurrentUserId();
        userService.createUser(req, callerRole, callerUserId);
        auditService.log(callerUserId, AuditAction.CREATE_USER.name(), AuditModule.IAM.name(), ClientIpUtil.resolve(httpReq));
        return ResponseEntity.status(201).body(ApiResponse.of("User created successfully", null));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADM', 'DS')")
    public ResponseEntity<ApiResponse> getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String callerRole = SecurityContextUtil.getCurrentRole();
        String callerUserId = SecurityContextUtil.getCurrentUserId();
        PageResponse<UserResponse> users = userService.getUsers(callerRole, callerUserId, role, status, departmentId, page, size);
        return ResponseEntity.ok(ApiResponse.data(users));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADM')")
    public ResponseEntity<ApiResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserStatusRequest req,
            HttpServletRequest httpReq) {
        String adminId = SecurityContextUtil.getCurrentUserId();
        userService.updateStatus(id, req.getStatus());
        auditService.log(adminId, AuditAction.UPDATE_STATUS.name(), AuditModule.IAM.name(), ClientIpUtil.resolve(httpReq));
        return ResponseEntity.ok(ApiResponse.of("User status updated to " + req.getStatus(), null));
    }
}
