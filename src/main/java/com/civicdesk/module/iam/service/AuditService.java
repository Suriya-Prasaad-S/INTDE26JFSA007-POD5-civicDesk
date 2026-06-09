package com.civicdesk.module.iam.service;

import com.civicdesk.common.response.PageResponse;
import com.civicdesk.module.iam.dto.response.AuditLogResponse;

public interface AuditService {

    /** Records a single audit entry. Call from every service method that changes data. */
    void log(String userId, String action, String module, String ip);

    PageResponse<AuditLogResponse> getAll(int page, int size);
}
