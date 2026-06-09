package com.civicdesk.module.iam.repository;

import com.civicdesk.module.iam.entity.AuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
        save("user-1", "LOGIN");
        save("user-1", "LOGOUT");
        save("user-2", "REGISTER");
    }

    private void save(String userId, String action) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setModule("IAM");
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
}
