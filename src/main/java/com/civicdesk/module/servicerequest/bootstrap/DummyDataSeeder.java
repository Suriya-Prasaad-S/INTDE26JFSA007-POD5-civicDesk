package com.civicdesk.module.serviceRequest.bootstrap;

import com.civicdesk.module.iam.entity.Department;
import com.civicdesk.module.iam.entity.User;
import com.civicdesk.module.iam.enums.Role;
import com.civicdesk.module.iam.enums.UserStatus;
import com.civicdesk.module.iam.repository.DepartmentRepository;
import com.civicdesk.module.iam.repository.UserRepository;
import com.civicdesk.module.serviceRequest.entity.ServiceCatalog;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceCategory;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceStatus;
import com.civicdesk.module.serviceRequest.entity.external.CitizenProfile;
import com.civicdesk.module.serviceRequest.repository.CitizenProfileRepository;
import com.civicdesk.module.serviceRequest.repository.ServiceCatalogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Demo data for the Service Request module, layered on top of the IAM module.
 *
 * <p>Now that IAM owns {@code users} and {@code departments}, this seeder no longer creates
 * those tables' rows from scratch. Instead it reuses an existing IAM department and adds the
 * Service-Request-specific demo data the endpoints need to be exercised end to end:
 * an active field officer (role {@code FO}) for auto-assignment, two citizen accounts
 * (one Active, one Suspended) with their {@code citizen_profile} rows, and a few
 * {@code service_catalog} services.</p>
 *
 * <p>Runs after {@link com.civicdesk.config.DataSeeder} (see {@link Order}) so the IAM
 * departments already exist, and adds <b>no</b> new department. Every block is idempotent,
 * so restarts and re-seeding are safe.</p>
 */
@Component
@Order(2)
public class DummyDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DummyDataSeeder.class);

    /** An IAM department (seeded by DataSeeder) the demo officer and services attach to. */
    private static final String DEMO_DEPARTMENT_NAME = "Citizen Services";

    private static final String OFFICER_EMAIL = "arjun.officer.demo@civicdesk.gov";
    private static final String CITIZEN_ACTIVE_EMAIL = "meena.citizen.demo@example.com";
    private static final String CITIZEN_SUSPENDED_EMAIL = "suresh.citizen.demo@example.com";

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final ServiceCatalogRepository catalogRepository;

    public DummyDataSeeder(DepartmentRepository departmentRepository,
                           UserRepository userRepository,
                           CitizenProfileRepository citizenProfileRepository,
                           ServiceCatalogRepository catalogRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.citizenProfileRepository = citizenProfileRepository;
        this.catalogRepository = catalogRepository;
    }

    @Override
    public void run(String... args) {
        Department department = resolveDemoDepartment();
        if (department == null) {
            log.warn("No IAM department available — skipping Service Request demo data seeding.");
            return;
        }
        seedOfficer(department.getDepartmentId());
        seedCitizens();
        seedCatalog(department);
    }

    /** Reuse an existing IAM department (never create one — IAM owns that table). */
    private Department resolveDemoDepartment() {
        return departmentRepository.findByName(DEMO_DEPARTMENT_NAME)
                .orElseGet(() -> departmentRepository.findAll().stream().findFirst().orElse(null));
    }

    /** A single active field officer in the demo department, used by auto-assignment. */
    private void seedOfficer(String departmentId) {
        if (userRepository.existsByEmail(OFFICER_EMAIL)) {
            return;
        }
        saveUser("Arjun Officer", OFFICER_EMAIL, "9000000004",
                Role.FO, UserStatus.ACT, departmentId);
    }

    /** Two citizen accounts (Active + Suspended) plus their citizen_profile rows. */
    private void seedCitizens() {
        String meenaId = ensureCitizen("Meena Citizen", CITIZEN_ACTIVE_EMAIL, "9111100001", UserStatus.ACT);
        String sureshId = ensureCitizen("Suresh Citizen", CITIZEN_SUSPENDED_EMAIL, "9111100002", UserStatus.SUS);

        if (citizenProfileRepository.count() == 0) {
            citizenProfileRepository.saveAll(List.of(
                    new CitizenProfile("citizen-0001", meenaId, "NID0001", "12 MG Road", "Ward-5", "Zone-A"),
                    new CitizenProfile("citizen-0002", sureshId, "NID0002", "7 Park Street", "Ward-3", "Zone-B")));
        }
    }

    /** Sample catalog services so submitRequest has known serviceIds to target. */
    private void seedCatalog(Department department) {
        if (catalogRepository.count() > 0) {
            return;
        }
        catalogRepository.saveAll(List.of(
                catalogService("svc-0001", "Birth Certificate", department, ServiceCategory.Certificate,
                        7, "[\"NationalID\",\"HospitalBirthRecord\",\"ParentID\"]", "150.00", ServiceStatus.Active),
                catalogService("svc-0002", "Income Certificate", department, ServiceCategory.Certificate,
                        10, "[\"NationalID\",\"SalarySlip\",\"ResidenceProof\"]", "100.00", ServiceStatus.Active),
                catalogService("svc-0003", "Drainage Connection", department, ServiceCategory.Utility,
                        14, "[\"NationalID\",\"ResidenceProof\",\"SiteMap\"]", "750.00", ServiceStatus.Inactive)));
    }

    /** Persist a citizen if absent and return its (generated) userId for profile linkage. */
    private String ensureCitizen(String name, String email, String phone, UserStatus status) {
        return userRepository.findByEmail(email)
                .map(User::getUserId)
                .orElseGet(() -> saveUser(name, email, phone, Role.CIT, status, null).getUserId());
    }

    private User saveUser(String name, String email, String phone,
                          Role role, UserStatus status, String departmentId) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(role.name());
        user.setStatus(status.getLabel());
        user.setDepartmentId(departmentId);
        user.setPasswordSet(false);
        return userRepository.save(user);
    }

    private ServiceCatalog catalogService(String id, String name, Department department,
                                          ServiceCategory category, int processingDays,
                                          String requiredDocsJson, String fee, ServiceStatus status) {
        ServiceCatalog service = new ServiceCatalog();
        service.setServiceId(id);
        service.setServiceName(name);
        service.setDepartment(department);
        service.setCategory(category);
        service.setProcessingDays(processingDays);
        service.setRequiredDocuments(requiredDocsJson);
        service.setFee(new BigDecimal(fee));
        service.setStatus(status);
        return service;
    }
}
