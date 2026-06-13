package com.civicdesk.module.iam.repository;

import com.civicdesk.module.iam.entity.AuditLog;
import com.civicdesk.module.iam.repository.spec.AuditLogSpecifications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setup() {
        save("user-1", "LOGIN", "IAM");
        save("user-1", "LOGOUT", "IAM");
        save("user-2", "REGISTER", "GRIEVANCE");
    }

    private void save(String userId, String action, String module) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setModule(module);
        log.setIpAddress("127.0.0.1");
        auditLogRepository.save(log);
    }

    @Test
    void findByUserId_returnsOnlyThatUsersEntries() {
        Page<AuditLog> page = auditLogRepository.findByUserId("user-1", PageRequest.of(0, 10));
        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream().allMatch(l -> l.getUserId().equals("user-1")));
    }

    @Test
    void findAllByOrderByTimestampDesc_returnsAllEntries() {
        Page<AuditLog> page = auditLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, 10));
        assertEquals(3, page.getTotalElements());
    }

    @Test
    void specification_filtersByAction() {
        Page<AuditLog> page = auditLogRepository.findAll(
                AuditLogSpecifications.hasAction("LOGIN"), PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
        assertEquals("LOGIN", page.getContent().get(0).getAction());
    }

    @Test
    void specification_filtersByModule() {
        Page<AuditLog> page = auditLogRepository.findAll(
                AuditLogSpecifications.hasModule("IAM"), PageRequest.of(0, 10));
        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream().allMatch(l -> l.getModule().equals("IAM")));
    }

    @Test
    void specification_filtersByLowercaseValue_isNormalized() {
        // action/module are stored upper-case; a lower-case filter must still match.
        Page<AuditLog> page = auditLogRepository.findAll(
                AuditLogSpecifications.hasModule("grievance"), PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void specification_combinesUserIdAndAction() {
        Specification<AuditLog> spec = AuditLogSpecifications.hasUserId("user-1")
                .and(AuditLogSpecifications.hasAction("LOGOUT"));
        Page<AuditLog> page = auditLogRepository.findAll(spec, PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
        assertEquals("user-1", page.getContent().get(0).getUserId());
        assertEquals("LOGOUT", page.getContent().get(0).getAction());
    }
}
