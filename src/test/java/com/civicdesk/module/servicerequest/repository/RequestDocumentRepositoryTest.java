package com.civicdesk.module.serviceRequest.repository;

import com.civicdesk.module.serviceRequest.entity.RequestDocument;
import com.civicdesk.module.serviceRequest.entity.ServiceCatalog;
import com.civicdesk.module.serviceRequest.entity.ServiceRequest;
import com.civicdesk.module.serviceRequest.entity.enums.RequestStatus;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceCategory;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceStatus;
import com.civicdesk.module.serviceRequest.entity.enums.VerificationStatus;
import com.civicdesk.module.serviceRequest.entity.external.CitizenProfile;
import com.civicdesk.module.serviceRequest.entity.external.Department;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Slice test for {@link RequestDocumentRepository#findByRequest_RequestId}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RequestDocumentRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private RequestDocumentRepository repository;

    @BeforeEach
    void seed() {
        Department dept = em.persist(new Department("DEP-A", "Revenue", "rev@city.gov"));
        CitizenProfile citizen = em.persist(new CitizenProfile("CIT-A", "USR-A", "NID", "addr", "W", "Z"));

        ServiceCatalog catalog = new ServiceCatalog();
        catalog.setServiceId("SVC-1");
        catalog.setServiceName("Birth Certificate");
        catalog.setDepartment(dept);
        catalog.setCategory(ServiceCategory.Certificate);
        catalog.setProcessingDays(7);
        catalog.setFee(new BigDecimal("50.00"));
        catalog.setStatus(ServiceStatus.Active);
        catalog.setRequiredDocuments("[]");
        em.persist(catalog);

        ServiceRequest reqWithDocs = request("REQ-1", citizen, catalog);
        ServiceRequest reqWithoutDocs = request("REQ-2", citizen, catalog);
        em.persist(reqWithDocs);
        em.persist(reqWithoutDocs);

        em.persist(document("DOC-1", reqWithDocs, "NationalID"));
        em.persist(document("DOC-2", reqWithDocs, "ResidenceProof"));
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("returns all documents uploaded against a request")
    void returnsDocumentsForRequest() {
        assertThat(repository.findByRequest_RequestId("REQ-1"))
                .extracting(RequestDocument::getDocId)
                .containsExactlyInAnyOrder("DOC-1", "DOC-2");
    }

    @Test
    @DisplayName("returns empty for a request with no documents")
    void emptyForRequestWithoutDocuments() {
        assertThat(repository.findByRequest_RequestId("REQ-2")).isEmpty();
    }

    private ServiceRequest request(String id, CitizenProfile citizen, ServiceCatalog service) {
        ServiceRequest r = new ServiceRequest();
        r.setRequestId(id);
        r.setCitizen(citizen);
        r.setService(service);
        r.setSubmissionDate(LocalDate.now());
        r.setFee(service.getFee());
        r.setExpectedCompletionDate(LocalDate.now().plusDays(7));
        r.setStatus(RequestStatus.Submitted);
        return r;
    }

    private RequestDocument document(String id, ServiceRequest request, String type) {
        RequestDocument d = new RequestDocument();
        d.setDocId(id);
        d.setRequest(request);
        d.setDocumentType(type);
        d.setFilePath("REQ-1/" + id + ".pdf");
        d.setUploadedDate(LocalDateTime.now());
        d.setVerificationStatus(VerificationStatus.Pending);
        return d;
    }
}
