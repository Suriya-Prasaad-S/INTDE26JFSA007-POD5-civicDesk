package com.civicdesk.module.auditlog.dto.request;

import com.civicdesk.module.auditlog.enums.AuditAction;
import com.civicdesk.module.auditlog.enums.AuditModule;
import com.civicdesk.module.auditlog.validation.EnumValid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code POST /audit/auditLogs}. {@code ipAddress} is deliberately absent —
 * it is resolved server-side from the request so callers cannot spoof it.
 */
public class CreateAuditLogRequest {

    @NotBlank(message = "userId is required")
    @Size(max = 20, message = "userId must not exceed 20 characters")
    private String userId;

    @NotBlank(message = "action is required")
    @EnumValid(enumClass = AuditAction.class, message = "action must be a valid audit action")
    private String action;

    @NotBlank(message = "module is required")
    @EnumValid(enumClass = AuditModule.class, message = "module must be a valid audit module")
    private String module;

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
}
