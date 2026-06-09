package com.civicdesk.module.iam.service;

import com.civicdesk.module.iam.entity.AuditLog;
import com.civicdesk.module.iam.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditServiceImpl auditService;

    @Test
    void log_savesAuditEntryWithGivenFields() {
        auditService.log("user-1", "LOGIN", "IAM", "10.0.0.1");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("user-1", saved.getUserId());
        assertEquals("LOGIN", saved.getAction());
        assertEquals("IAM", saved.getModule());
        assertEquals("10.0.0.1", saved.getIpAddress());
    }
}
