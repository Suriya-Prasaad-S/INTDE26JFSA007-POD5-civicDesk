package com.civicdesk.module.auditlog.dto.response;

import com.civicdesk.module.auditlog.entity.AuditLog;

import java.time.LocalDateTime;

public class AuditLogResponse {

    private String auditId;
    private String userId;
    private String action;
    private String module;
    private String ipAddress;
    private LocalDateTime timestamp;

    public AuditLogResponse() {
    }

    public AuditLogResponse(String auditId, String userId, String action,
                            String module, String ipAddress, LocalDateTime timestamp) {
        this.auditId = auditId;
        this.userId = userId;
        this.action = action;
        this.module = module;
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
    }

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getAuditId(),
                log.getUserId(),
                log.getAction(),
                log.getModule(),
                log.getIpAddress(),
                log.getTimestamp());
    }

    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
