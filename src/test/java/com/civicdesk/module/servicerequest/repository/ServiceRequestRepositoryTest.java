package com.civicdesk.module.serviceRequest.repository;

import com.civicdesk.module.serviceRequest.entity.ServiceCatalog;
import com.civicdesk.module.serviceRequest.entity.ServiceRequest;
import com.civicdesk.module.serviceRequest.entity.enums.RequestStatus;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceCategory;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceStatus;
import com.civicdesk.module.serviceRequest.entity.external.CitizenProfile;
import com.civicdesk.module.serviceRequest.entity.external.Department;
import com.civicdesk.module.serviceRequest.entity.external.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Slice tests for {@link ServiceRequestRepository} derived queries, run against the
 * configured H2 datasource (see src/test/resources/application.properties).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ServiceRequestRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private ServiceRequestRepository repository;

    private CitizenProfile citizenA;
    private CitizenProfile citizenB;
    private User officer;
    private ServiceCatalog catalogDeptA;
    private ServiceCatalog catalogDeptB;

    @BeforeEach
    void seed() {
        Department deptA = em.persist(new Department("DEP-A", "Revenue", "rev@city.gov"));
        Department deptB = em.persist(new Department("DEP-B", "Works", "works@city.gov"));

        citizenA = em.persist(new CitizenProfile("CIT-A", "USR-A", "NID-A", "addr", "W1", "Z1"));
        citizenB = em.persist(new CitizenProfile("CIT-B", "USR-B", "NID-B", "addr", "W2", "Z2"));
        officer = em.persist(new User("OFF-1", "Olivia", "olivia@city.gov", "555", "Officer", "DEP-A", "A"));

        catalogDeptA = em.persist(catalog("SVC-A", "Birth Certificate", deptA, ServiceCategory.Certificate));
        catalogDeptB = em.persist(catalog("SVC-B", "Drainage", deptB, ServiceCategory.Utility));

        // citizenA: 2 requests in DEP-A (Submitted, Completed); citizenB: 1 in DEP-B (UnderReview).
        em.persist(request("REQ-1", citizenA, catalogDeptA, RequestStatus.Submitted));
        em.persist(request("REQ-2", citizenA, catalogDeptA, RequestStatus.Completed));
        em.persist(request("REQ-3", citizenB, catalogDeptB, RequestStatus.UnderReview));
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("findByStatus returns only requests in that status")
    void findByStatus() {
        assertThat(repository.findByStatus(RequestStatus.Submitted))
                .extracting(ServiceRequest::getRequestId)
                .containsExactly("REQ-1");
    }

    @Test
    @DisplayName("findByService_Department_DepartmentId filters by the service's department")
    void findByDepartment() {
        assertThat(repository.findByService_Department_DepartmentId("DEP-A"))
                .extracting(ServiceRequest::getRequestId)
                .containsExactlyInAnyOrder("REQ-1", "REQ-2");
    }

    @Test
    @DisplayName("findByStatusAndService_Department_DepartmentId applies both filters")
    void findByStatusAndDepartment() {
        assertThat(repository.findByStatusAndService_Department_DepartmentId(
                RequestStatus.Completed, "DEP-A"))
                .extracting(ServiceRequest::getRequestId)
                .containsExactly("REQ-2");

        assertThat(repository.findByStatusAndService_Department_DepartmentId(
                RequestStatus.Submitted, "DEP-B")).isEmpty();
    }

    @Test
    @DisplayName("findByCitizen_CitizenId returns all of a citizen's requests")
    void findByCitizen() {
        assertThat(repository.findByCitizen_CitizenId("CIT-A"))
                .extracting(ServiceRequest::getRequestId)
                .containsExactlyInAnyOrder("REQ-1", "REQ-2");
    }

    @Test
    @DisplayName("countByAssignedOfficerAndStatusNotIn excludes terminal requests from the workload")
    void countActiveWorkload() {
        User managed = em.find(User.class, "OFF-1");
        // All three requests are assigned to OFF-1: REQ-1 (Submitted) and REQ-3 (UnderReview)
        // are non-terminal; REQ-2 (Completed) is terminal and must not count.
        long active = repository.countByAssignedOfficerAndStatusNotIn(
                managed, List.of(RequestStatus.Completed, RequestStatus.Rejected));
        assertThat(active).isEqualTo(2);
    }

    private ServiceCatalog catalog(String id, String name, Department dept, ServiceCategory category) {
        ServiceCatalog c = new ServiceCatalog();
        c.setServiceId(id);
        c.setServiceName(name);
        c.setDepartment(dept);
        c.setCategory(category);
        c.setProcessingDays(7);
        c.setFee(new BigDecimal("50.00"));
        c.setStatus(ServiceStatus.Active);
        c.setRequiredDocuments("[]");
        return c;
    }

    private ServiceRequest request(String id, CitizenProfile citizen, ServiceCatalog service,
                                   RequestStatus status) {
        ServiceRequest r = new ServiceRequest();
        r.setRequestId(id);
        r.setCitizen(citizen);
        r.setService(service);
        r.setSubmissionDate(LocalDate.now());
        r.setAssignedOfficer(officer);
        r.setFee(service.getFee());
        r.setExpectedCompletionDate(LocalDate.now().plusDays(7));
        r.setStatus(status);
        return r;
    }
}
