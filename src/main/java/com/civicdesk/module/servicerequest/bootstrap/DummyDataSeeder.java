package com.civicdesk.module.serviceRequest.bootstrap;

import com.civicdesk.module.serviceRequest.entity.ServiceCatalog;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceCategory;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceStatus;
import com.civicdesk.module.serviceRequest.entity.external.CitizenProfile;
import com.civicdesk.module.serviceRequest.entity.external.Department;
import com.civicdesk.module.serviceRequest.entity.external.User;
import com.civicdesk.module.serviceRequest.repository.CitizenProfileRepository;
import com.civicdesk.module.serviceRequest.repository.DepartmentRepository;
import com.civicdesk.module.serviceRequest.repository.ServiceCatalogRepository;
import com.civicdesk.module.serviceRequest.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * TEMPORARY startup seeder — the Java replacement for the old {@code data.sql}.
 *
 * <p>Populates the placeholder cross-module tables ({@code departments}, {@code users},
 * {@code citizen_profile}) plus a couple of sample {@code service_catalog} rows so the
 * three POST endpoints can be exercised end-to-end before the IAM / Citizen modules exist.
 * It is idempotent: each block only runs when its table is empty, so restarts are safe.</p>
 *
 * <p><b>Delete this class (and the {@code entity/external} package) once the real IAM /
 * Citizen modules land</b> — they will own and seed these tables. See
 * {@code docs/serviceRequest-module.md}.</p>
 */
@Component
public class DummyDataSeeder implements CommandLineRunner {

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
        seedDepartments();
        seedUsers();
        seedCitizenProfiles();
        seedCatalog();
    }

    private void seedDepartments() {
        if (departmentRepository.count() > 0) {
            return;
        }
        departmentRepository.saveAll(List.of(
                new Department("dept-0004", "Citizen Services", "citizenservices@civicdesk.gov"),
                new Department("dept-0002", "Public Works", "publicworks@civicdesk.gov")));
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.saveAll(List.of(
                // Officers — used by auto-assignment.
                new User("off-0004", "Arjun Officer", "arjun@civicdesk.gov", "9000000004", "Officer", "dept-0004", "A"),
                new User("off-0006", "Priya Officer", "priya@civicdesk.gov", "9000000006", "Officer", "dept-0004", "A"),
                new User("off-0002", "Ravi Officer", "ravi@civicdesk.gov", "9000000002", "Officer", "dept-0002", "A"),
                // Citizens — one Active, one Flagged (to exercise the 403 path).
                new User("usr-c001", "Meena Citizen", "meena@example.com", "9111100001", "Citizen", null, "A"),
                new User("usr-c002", "Suresh Citizen", "suresh@example.com", "9111100002", "Citizen", null, "F")));}

    private void seedCitizenProfiles() {
        if (citizenProfileRepository.count() > 0) {
            return;
        }
        citizenProfileRepository.saveAll(List.of(
                new CitizenProfile("citizen-0001", "usr-c001", "NID0001", "12 MG Road", "Ward-5", "Zone-A"),
                new CitizenProfile("citizen-0002", "usr-c002", "NID0002", "7 Park Street", "Ward-3", "Zone-B")));
    }

    /** Sample catalog services so {@code submitRequest} has a known serviceId to target. */
    private void seedCatalog() {
        if (catalogRepository.count() > 0) {
            return;
        }
        Department citizenServices = departmentRepository.findById("dept-0004").orElseThrow();
        Department publicWorks = departmentRepository.findById("dept-0002").orElseThrow();

        catalogRepository.saveAll(List.of(
                catalogService("svc-0001", "Birth Certificate", citizenServices, ServiceCategory.Certificate,
                        7, "[\"NationalID\",\"HospitalBirthRecord\",\"ParentID\"]", "150.00", ServiceStatus.Active),
                catalogService("svc-0002", "Income Certificate", citizenServices, ServiceCategory.Certificate,
                        10, "[\"NationalID\",\"SalarySlip\",\"ResidenceProof\"]", "100.00", ServiceStatus.Active),
                catalogService("svc-0003", "Drainage Connection", publicWorks, ServiceCategory.Utility,
                        14, "[\"NationalID\",\"ResidenceProof\",\"SiteMap\"]", "750.00", ServiceStatus.Inactive)));
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
