package com.civicdesk.module.auditlog.repository;

import com.civicdesk.module.auditlog.entity.AuditLog;
import com.civicdesk.module.auditlog.repository.spec.AuditLogSpecifications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    private AuditLog save(String userId, String action, String module) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setModule(module);
        log.setIpAddress("127.0.0.1");
        return auditLogRepository.save(log);
    }

    // ---- id / timestamp generation -----------------------------------------

    @Test
    void save_assignsNumericStringAuditId() {
        AuditLog saved = save("user-9", "LOGIN", "IAM");
        assertNotNull(saved.getAuditId());
        assertTrue(saved.getAuditId().matches("\\d+"), "auditId should be numeric: " + saved.getAuditId());
    }

    @Test
    void save_assignsCreationTimestamp() {
        AuditLog saved = save("user-9", "LOGIN", "IAM");
        auditLogRepository.flush(); // @CreationTimestamp is populated at INSERT time
        assertNotNull(saved.getTimestamp());
    }

    @Test
    void findById_returnsSavedEntry() {
        AuditLog saved = save("user-9", "LOGIN", "IAM");
        assertTrue(auditLogRepository.findById(saved.getAuditId()).isPresent());
    }

    @Test
    void count_reflectsSeededRows() {
        assertEquals(3, auditLogRepository.count());
    }

    // ---- derived queries ----------------------------------------------------

    @Test
    void findByUserId_returnsOnlyThatUsersEntries() {
        Page<AuditLog> page = auditLogRepository.findByUserId("user-1", PageRequest.of(0, 10));
        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream().allMatch(l -> l.getUserId().equals("user-1")));
    }

    @Test
    void findByUserId_unknownUser_returnsEmpty() {
        Page<AuditLog> page = auditLogRepository.findByUserId("nobody", PageRequest.of(0, 10));
        assertEquals(0, page.getTotalElements());
    }

    @Test
    void findByModule_returnsOnlyThatModule() {
        Page<AuditLog> page = auditLogRepository.findByModule("IAM", PageRequest.of(0, 10));
        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream().allMatch(l -> l.getModule().equals("IAM")));
    }

    @Test
    void findAllByOrderByTimestampDesc_returnsAllEntries() {
        Page<AuditLog> page = auditLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, 10));
        assertEquals(3, page.getTotalElements());
    }

    @Test
    void findAll_respectsPageSize() {
        Page<AuditLog> page = auditLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, 2));
        assertEquals(2, page.getContent().size());
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }

    // ---- specifications -----------------------------------------------------

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
    void specification_filtersByUserId() {
        Page<AuditLog> page = auditLogRepository.findAll(
                AuditLogSpecifications.hasUserId("user-2"), PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
        assertEquals("user-2", page.getContent().get(0).getUserId());
    }

    @Test
    void specification_filtersByLowercaseModule_isNormalized() {
        Page<AuditLog> page = auditLogRepository.findAll(
                AuditLogSpecifications.hasModule("grievance"), PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void specification_filtersByLowercaseAction_isNormalized() {
        Page<AuditLog> page = auditLogRepository.findAll(
                AuditLogSpecifications.hasAction("login"), PageRequest.of(0, 10));
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

    @Test
    void specification_combinesAllThree() {
        Specification<AuditLog> spec = AuditLogSpecifications.hasUserId("user-1")
                .and(AuditLogSpecifications.hasAction("LOGIN"))
                .and(AuditLogSpecifications.hasModule("IAM"));
        Page<AuditLog> page = auditLogRepository.findAll(spec, PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void specification_noMatch_returnsEmpty() {
        Specification<AuditLog> spec = AuditLogSpecifications.hasUserId("user-2")
                .and(AuditLogSpecifications.hasModule("IAM"));
        Page<AuditLog> page = auditLogRepository.findAll(spec, PageRequest.of(0, 10));
        assertEquals(0, page.getTotalElements());
    }

    @Test
    void specification_withSort_appliesOrdering() {
        Page<AuditLog> page = auditLogRepository.findAll(
                AuditLogSpecifications.hasModule("IAM"),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "action")));
        assertEquals("LOGIN", page.getContent().get(0).getAction());
        assertEquals("LOGOUT", page.getContent().get(1).getAction());
    }
}
