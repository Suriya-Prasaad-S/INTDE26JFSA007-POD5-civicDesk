package com.civicdesk.module.iam.controller;

import com.civicdesk.common.response.ApiResponse;
import com.civicdesk.common.response.PageResponse;
import com.civicdesk.module.iam.dto.response.AuditLogResponse;
import com.civicdesk.module.iam.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auditLogs")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADM', 'CO')")
    public ResponseEntity<ApiResponse> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<AuditLogResponse> logs = auditService.getAll(page, size);
        return ResponseEntity.ok(ApiResponse.data(logs));
    }
}
