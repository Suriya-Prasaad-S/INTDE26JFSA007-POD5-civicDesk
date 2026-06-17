package com.civicdesk.module.iam.integration;

import com.civicdesk.module.iam.entity.User;
import com.civicdesk.module.auditlog.repository.AuditLogRepository;
import com.civicdesk.module.iam.repository.DepartmentRepository;
import com.civicdesk.module.iam.repository.UserRepository;
import com.civicdesk.module.auditlog.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the id scheme: {@code userId} / {@code auditId} are sequential numeric
 * strings beginning at 10000001 (replacing UUIDs), while department ids use the
 * {@code DPT##} business-key form assigned by {@code DataSeeder}.
 */
@SpringBootTest
@ActiveProfiles("test")
class IdGenerationIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private AuditService auditService;
    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void seededAdmin_getsFirstSequentialUserId() {
        // The seeded ADM is the first row inserted into users, so it takes the start value.
        User admin = userRepository.findByEmail("admin@civicdesk.gov").orElseThrow();
        assertThat(admin.getUserId()).isEqualTo("10000001");
    }

    @Test
    void persistedUsers_getConsecutiveNumericStringIds() {
        User a = newCitizen("seq.a@idtest.com");
        User b = newCitizen("seq.b@idtest.com");
        userRepository.saveAndFlush(a);
        userRepository.saveAndFlush(b);

        assertThat(a.getUserId()).matches("\\d+");
        assertThat(b.getUserId()).matches("\\d+");
        assertThat(Long.parseLong(a.getUserId())).isGreaterThanOrEqualTo(10_000_001L);
        assertThat(Long.parseLong(b.getUserId())).isEqualTo(Long.parseLong(a.getUserId()) + 1);
    }

    @Test
    void auditLog_getsNumericStringIds() {
        auditService.log("10000001", "TEST_ACTION", "IAM", "127.0.0.1");
        assertThat(auditLogRepository.findAll())
                .isNotEmpty()
                .allSatisfy(entry -> assertThat(entry.getAuditId()).matches("\\d+"));
    }

    @Test
    void seededDepartments_useDptPrefixedIds() {
        assertThat(departmentRepository.findAll())
                .hasSize(6)
                .allSatisfy(d -> assertThat(d.getDepartmentId()).matches("DPT\\d{2,}"));
        // List order in the seeder determines the suffix: "Infrastructure" is first → DPT01.
        assertThat(departmentRepository.findByName("Infrastructure").orElseThrow().getDepartmentId())
                .isEqualTo("DPT01");
    }

    private User newCitizen(String email) {
        User u = new User();
        u.setName("Seq Test");
        u.setEmail(email);
        u.setPhone("9876543210");
        u.setRole("CIT");
        u.setStatus("A");
        u.setPasswordSet(true);
        return u;
    }
}
