package com.civicdesk.module.auditlog.service;

import com.civicdesk.common.exception.ResourceNotFoundException;
import com.civicdesk.common.response.PageResponse;
import com.civicdesk.module.auditlog.dto.response.AuditLogResponse;
import com.civicdesk.module.auditlog.entity.AuditLog;
import com.civicdesk.module.auditlog.repository.AuditLogRepository;
import com.civicdesk.module.auditlog.repository.spec.AuditLogSpecifications;
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
        save(userId, action, module, ip);
    }

    @Override
    public AuditLogResponse create(String userId, String action, String module, String ip) {
        return AuditLogResponse.from(save(userId, action, module, ip));
    }

    /**
     * Persists a single audit row. action/module are normalised to upper case so the
     * stored value always matches the canonical enum name the GET filters compare against.
     */
    private AuditLog save(String userId, String action, String module, String ip) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action != null ? action.trim().toUpperCase() : null);
        log.setModule(module != null ? module.trim().toUpperCase() : null);
        log.setIpAddress(ip);
        return auditLogRepository.save(log);
    }

    @Override
    public PageResponse<AuditLogResponse> getAll(String userId, String action, String module, int page, int size) {
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

    @Override
    public AuditLogResponse getById(String auditId) {
        AuditLog log = auditLogRepository.findById(auditId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found"));
        return AuditLogResponse.from(log);
    }
}
