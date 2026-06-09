package com.civicdesk.module.iam.service;

import com.civicdesk.common.response.PageResponse;
import com.civicdesk.module.iam.dto.response.AuditLogResponse;
import com.civicdesk.module.iam.entity.AuditLog;
import com.civicdesk.module.iam.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void log(String userId, String action, String module, String ip) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setModule(module);
        log.setIpAddress(ip);
        auditLogRepository.save(log);
    }

    @Override
    public PageResponse<AuditLogResponse> getAll(int page, int size) {
        Page<AuditLog> logs = auditLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(page, size));
        return PageResponse.from(logs, AuditLogResponse::from);
    }
}
