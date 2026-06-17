package com.civicdesk.module.auditlog.controller;

import com.civicdesk.common.response.ApiResponse;
import com.civicdesk.common.response.PageResponse;
import com.civicdesk.common.util.ClientIpUtil;
import com.civicdesk.module.auditlog.dto.request.CreateAuditLogRequest;
import com.civicdesk.module.auditlog.dto.response.AuditLogResponse;
import com.civicdesk.module.auditlog.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit/auditLogs")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Records a new audit entry. Open to any authenticated principal; the client IP is
     * resolved server-side, and action/module are validated against the audit enums.
     */
    @PostMapping
    public ResponseEntity<ApiResponse> createAuditLog(
            @Valid @RequestBody CreateAuditLogRequest req,
            HttpServletRequest httpReq) {
        String ip = ClientIpUtil.resolve(httpReq);
        AuditLogResponse created = auditService.create(req.getUserId(), req.getAction(), req.getModule(), ip);
        return ResponseEntity.status(201).body(ApiResponse.of("Audit log recorded successfully", created));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADM', 'CO')")
    public ResponseEntity<ApiResponse> getAuditLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<AuditLogResponse> logs = auditService.getAll(userId, action, module, page, size);
        return ResponseEntity.ok(ApiResponse.data(logs));
    }

    @GetMapping("/{auditId}")
    @PreAuthorize("hasAnyRole('ADM', 'CO')")
    public ResponseEntity<ApiResponse> getAuditLogById(@PathVariable String auditId) {
        AuditLogResponse log = auditService.getById(auditId);
        return ResponseEntity.ok(ApiResponse.data(log));
    }
}
