package com.civicdesk.module.auditlog.service;

import com.civicdesk.common.response.PageResponse;
import com.civicdesk.module.auditlog.dto.response.AuditLogResponse;

public interface AuditService {

    /** Fire-and-forget internal logging used by other modules; returns nothing. */
    void log(String userId, String action, String module, String ip);

    /** Records an audit entry and returns the persisted row (used by the POST endpoint). */
    AuditLogResponse create(String userId, String action, String module, String ip);

    PageResponse<AuditLogResponse> getAll(String userId, String action, String module, int page, int size);

    AuditLogResponse getById(String auditId);
}
