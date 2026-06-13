package com.civicdesk.config;

import com.civicdesk.module.iam.entity.Department;
import com.civicdesk.module.iam.entity.User;
import com.civicdesk.module.iam.enums.AuditAction;
import com.civicdesk.module.iam.enums.AuditModule;
import com.civicdesk.module.iam.enums.Role;
import com.civicdesk.module.iam.enums.UserStatus;
import com.civicdesk.module.iam.repository.DepartmentRepository;
import com.civicdesk.module.iam.repository.UserRepository;
import com.civicdesk.module.iam.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the very first ADM account directly into the database on application
 * startup â€” bypassing the API entirely, since no admin exists yet to call it.
 *
 * <p>Admin details come from {@code application.properties}, where each value is
 * driven by an OS environment variable with a local-development default (e.g.
 * {@code app.admin.email=${ADMIN_EMAIL:admin@civicdesk.gov}}). Override them in
 * any real environment by exporting {@code ADMIN_EMAIL}, {@code ADMIN_PASSWORD},
 * {@code ADMIN_NAME}, and {@code ADMIN_PHONE}.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.name}")
    private String adminName;

    @Value("${app.admin.phone}")
    private String adminPhone;

    /** The six municipal departments CivicDesk operates. */
    private static final List<String> DEPARTMENTS = List.of(
            "Infrastructure",
            "Public Health",
            "Licensing & Compliance",
            "Citizen Services",
            "Administration",
            "Compliance & Audit");

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public DataSeeder(UserRepository userRepository,
                      DepartmentRepository departmentRepository,
                      PasswordEncoder passwordEncoder,
                      AuditService auditService) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    public void run(String... args) {
        seedDepartments();
        seedAdmin();
    }

    private void seedDepartments() {
        for (String name : DEPARTMENTS) {
            if (!departmentRepository.existsByName(name)) {
                Department dept = new Department(name);
                dept.setDepartmentId(nextDepartmentId());
                departmentRepository.save(dept);
                log.info("Seeded department '{}' as '{}'.", name, dept.getDepartmentId());
            }
        }
    }

    /**
     * Builds the next sequential department id ({@code DPT01}, {@code DPT02}, …) by
     * scanning existing ids for the highest {@code DPT<n>} suffix and incrementing it.
     * Ids that don't match the pattern are ignored, so the sequence is stable even if
     * other id forms ever coexist.
     */
    private String nextDepartmentId() {
        int max = departmentRepository.findAll().stream()
                .map(Department::getDepartmentId)
                .filter(id -> id != null && id.matches("DPT\\d+"))
                .mapToInt(id -> Integer.parseInt(id.substring(3)))
                .max()
                .orElse(0);
        return String.format("DPT%02d", max + 1);
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Default ADM '{}' already present â€” skipping seed.", adminEmail);
            return;
        }

        User admin = new User();
        admin.setName(adminName);
        admin.setEmail(adminEmail);
        admin.setPhone(adminPhone);
        admin.setRole(Role.ADM.name());
        admin.setStatus(UserStatus.ACT.getLabel());
        admin.setDepartmentId(null);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setPasswordSet(true); 
        userRepository.save(admin);

        auditService.log(admin.getUserId(), AuditAction.SEED_ADMIN.name(), AuditModule.IAM.name(), "system");
        log.info("Seeded default ADM account '{}'.", adminEmail);
    }
}
