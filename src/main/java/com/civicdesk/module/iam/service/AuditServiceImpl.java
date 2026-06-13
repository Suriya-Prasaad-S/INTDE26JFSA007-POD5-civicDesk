package com.civicdesk.module.iam.service;

import com.civicdesk.common.response.PageResponse;
import com.civicdesk.module.iam.dto.response.AuditLogResponse;
import com.civicdesk.module.iam.entity.AuditLog;
import com.civicdesk.module.iam.repository.AuditLogRepository;
import com.civicdesk.module.iam.repository.spec.AuditLogSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    public PageResponse<AuditLogResponse> getAll(String userId, String action, String module, int page, int size) {
        // Compose only the filters that were supplied; newest-first ordering is applied
        // via the pageable so it works alongside the optional specification.
        Specification<AuditLog> spec = Specification.where(null);
        if (userId != null && !userId.isBlank()) {
            spec = spec.and(AuditLogSpecifications.hasUserId(userId.trim()));
        }
        if (action != null && !action.isBlank()) {
            spec = spec.and(AuditLogSpecifications.hasAction(action));
        }
        if (module != null && !module.isBlank()) {
            spec = spec.and(AuditLogSpecifications.hasModule(module));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> logs = auditLogRepository.findAll(spec, pageable);
        return PageResponse.from(logs, AuditLogResponse::from);
    }
}
