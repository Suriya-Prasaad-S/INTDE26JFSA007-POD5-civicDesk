package com.civicdesk.module.iam.controller;

import com.civicdesk.common.response.ApiResponse;
import com.civicdesk.common.util.ClientIpUtil;
import com.civicdesk.common.util.SecurityContextUtil;
import com.civicdesk.module.iam.dto.request.CitizenLoginRequest;
import com.civicdesk.module.iam.dto.request.RegisterRequest;
import com.civicdesk.module.iam.dto.request.SetPasswordRequest;
import com.civicdesk.module.iam.dto.request.StaffLoginRequest;
import com.civicdesk.module.iam.dto.response.AuthResponse;
import com.civicdesk.module.iam.enums.AuditAction;
import com.civicdesk.module.iam.enums.AuditModule;
import com.civicdesk.module.iam.service.AuditService;
import com.civicdesk.module.iam.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/iam/auth")
public class AuthController {

    private final AuthService authService;
    private final AuditService auditService;

    public AuthController(AuthService authService, AuditService auditService) {
        this.authService = authService;
        this.auditService = auditService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(
            @Valid @RequestBody RegisterRequest req,
            HttpServletRequest httpReq) {
        authService.register(req, ClientIpUtil.resolve(httpReq));
        return ResponseEntity.status(201).body(ApiResponse.of("Registration successful", null));
    }

    @PostMapping("/citizen/login")
    public ResponseEntity<ApiResponse> citizenLogin(
            @Valid @RequestBody CitizenLoginRequest req,
            HttpServletRequest httpReq) {
        AuthResponse res = authService.citizenLogin(req, ClientIpUtil.resolve(httpReq));
        return ResponseEntity.ok(ApiResponse.of("Login successful", res));
    }

    @PostMapping("/staff/login")
    public ResponseEntity<ApiResponse> staffLogin(
            @Valid @RequestBody StaffLoginRequest req,
            HttpServletRequest httpReq) {
        AuthResponse res = authService.staffLogin(req, ClientIpUtil.resolve(httpReq));
        return ResponseEntity.ok(ApiResponse.of("Login successful", res));
    }

    @PostMapping("/setPassword")
    public ResponseEntity<ApiResponse> setPassword(@Valid @RequestBody SetPasswordRequest req) {
        authService.setPassword(req);
        return ResponseEntity.ok(ApiResponse.of("Password set successfully. Please login.", null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpServletRequest httpReq) {
        String userId = SecurityContextUtil.getCurrentUserId();
        auditService.log(userId, AuditAction.LOGOUT.name(), AuditModule.IAM.name(), ClientIpUtil.resolve(httpReq));
        return ResponseEntity.ok(ApiResponse.of("Logout successful", null));
    }
}
