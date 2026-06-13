package com.civicdesk.module.serviceRequest.repository;

import com.civicdesk.module.serviceRequest.entity.ServiceCatalog;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceCategory;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceStatus;
import com.civicdesk.module.serviceRequest.entity.external.Department;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Slice tests for {@link ServiceCatalogRepository} derived queries.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ServiceCatalogRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private ServiceCatalogRepository repository;

    @BeforeEach
    void seed() {
        Department dept = em.persist(new Department("DEP-A", "Revenue", "rev@city.gov"));
        em.persist(catalog("SVC-1", "Birth Certificate", dept, ServiceCategory.Certificate, ServiceStatus.Active));
        em.persist(catalog("SVC-2", "Income Certificate", dept, ServiceCategory.Certificate, ServiceStatus.Active));
        em.persist(catalog("SVC-3", "Water Connection", dept, ServiceCategory.Utility, ServiceStatus.Active));
        em.persist(catalog("SVC-4", "Old Service", dept, ServiceCategory.Certificate, ServiceStatus.Inactive));
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("existsByServiceNameIgnoreCase matches regardless of case")
    void existsByServiceNameIgnoreCase() {
        assertThat(repository.existsByServiceNameIgnoreCase("birth CERTIFICATE")).isTrue();
        assertThat(repository.existsByServiceNameIgnoreCase("Nonexistent")).isFalse();
    }

    @Test
    @DisplayName("findByStatus returns all Active services and excludes Inactive ones")
    void findByStatus() {
        assertThat(repository.findByStatus(ServiceStatus.Active))
                .extracting(ServiceCatalog::getServiceId)
                .containsExactlyInAnyOrder("SVC-1", "SVC-2", "SVC-3");
        assertThat(repository.findByStatus(ServiceStatus.Inactive))
                .extracting(ServiceCatalog::getServiceId)
                .containsExactly("SVC-4");
    }

    @Test
    @DisplayName("findByStatusAndCategory filters by status and category")
    void findByStatusAndCategory() {
        assertThat(repository.findByStatusAndCategory(ServiceStatus.Active, ServiceCategory.Certificate))
                .extracting(ServiceCatalog::getServiceId)
                .containsExactlyInAnyOrder("SVC-1", "SVC-2");
        assertThat(repository.findByStatusAndCategory(ServiceStatus.Active, ServiceCategory.Welfare))
                .isEmpty();
    }

    private ServiceCatalog catalog(String id, String name, Department dept,
                                   ServiceCategory category, ServiceStatus status) {
        ServiceCatalog c = new ServiceCatalog();
        c.setServiceId(id);
        c.setServiceName(name);
        c.setDepartment(dept);
        c.setCategory(category);
        c.setProcessingDays(7);
        c.setFee(new BigDecimal("50.00"));
        c.setStatus(status);
        c.setRequiredDocuments("[]");
        return c;
    }
}
