package com.civicdesk.module.serviceRequest.service;

import com.civicdesk.common.exception.ResourceNotFoundException;
import com.civicdesk.common.exception.UnprocessableEntityException;
import com.civicdesk.module.serviceRequest.dto.request.SubmitServiceRequest;
import com.civicdesk.module.serviceRequest.dto.request.UpdateRequestStatusRequest;
import com.civicdesk.module.serviceRequest.dto.response.CitizenRequestItemResponse;
import com.civicdesk.module.serviceRequest.dto.response.DocumentItemResponse;
import com.civicdesk.module.serviceRequest.dto.response.MessageResponse;
import com.civicdesk.module.serviceRequest.dto.response.RequestDetailResponse;
import com.civicdesk.module.serviceRequest.dto.response.RequestListItemResponse;
import com.civicdesk.module.serviceRequest.entity.RequestDocument;
import com.civicdesk.module.serviceRequest.entity.ServiceCatalog;
import com.civicdesk.module.serviceRequest.entity.ServiceRequest;
import com.civicdesk.module.serviceRequest.entity.enums.RequestStatus;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceCategory;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceStatus;
import com.civicdesk.module.serviceRequest.entity.enums.VerificationStatus;
import com.civicdesk.module.serviceRequest.entity.external.CitizenProfile;
import com.civicdesk.module.serviceRequest.entity.external.Department;
import com.civicdesk.module.serviceRequest.entity.external.User;
import com.civicdesk.module.notification.dto.request.NotificationRequestDTO;
import com.civicdesk.module.notification.entity.enums.Category;
import com.civicdesk.module.notification.service.NotificationService;
import com.civicdesk.module.serviceRequest.repository.RequestDocumentRepository;
import com.civicdesk.module.serviceRequest.repository.ServiceCatalogRepository;
import com.civicdesk.module.serviceRequest.repository.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ServiceRequestService} business rules: submission snapshotting,
 * filtered queries, detail mapping, and the status-transition workflow. Repositories and
 * collaborators are mocked so only the service logic is exercised.
 */
@ExtendWith(MockitoExtension.class)
class ServiceRequestServiceTest {

    @Mock private ServiceRequestRepository requestRepository;
    @Mock private ServiceCatalogRepository catalogRepository;
    @Mock private RequestDocumentRepository documentRepository;
    @Mock private CitizenLookup citizenLookup;
    @Mock private OfficerAssignment officerAssignment;
    @Mock private NotificationService notificationService;

    @InjectMocks private ServiceRequestService service;

    private Department department;
    private CitizenProfile citizen;
    private User officer;
    private ServiceCatalog catalog;

    @BeforeEach
    void setUp() {
        department = new Department("DEP-1", "Revenue", "revenue@city.gov");

        citizen = new CitizenProfile("CIT-1", "USR-1", "NID-1", "1 Main St", "W1", "Z1");

        officer = new User("OFF-1", "Olivia Officer", "olivia@city.gov", "555-0100",
                "Officer", "DEP-1", "A");

        catalog = new ServiceCatalog();
        catalog.setServiceId("SVC-1");
        catalog.setServiceName("Birth Certificate");
        catalog.setDepartment(department);
        catalog.setCategory(ServiceCategory.Certificate);
        catalog.setProcessingDays(7);
        catalog.setFee(new BigDecimal("50.00"));
        catalog.setStatus(ServiceStatus.Active);
    }

    @Nested
    @DisplayName("submitRequest")
    class SubmitRequest {

        @Test
        @DisplayName("snapshots fee, computes completion date, assigns officer and saves as Submitted")
        void submitsSuccessfully() {
            SubmitServiceRequest req = new SubmitServiceRequest("CIT-1", "SVC-1");
            when(citizenLookup.loadSubmittableCitizen("CIT-1")).thenReturn(citizen);
            when(catalogRepository.findById("SVC-1")).thenReturn(Optional.of(catalog));
            when(officerAssignment.findLeastLoadedOfficer("DEP-1")).thenReturn(officer);

            MessageResponse response = service.submitRequest(req);

            ArgumentCaptor<ServiceRequest> captor = ArgumentCaptor.forClass(ServiceRequest.class);
            verify(requestRepository).save(captor.capture());
            ServiceRequest saved = captor.getValue();

            assertThat(saved.getRequestId()).isNotBlank();
            assertThat(saved.getCitizen()).isSameAs(citizen);
            assertThat(saved.getService()).isSameAs(catalog);
            assertThat(saved.getAssignedOfficer()).isSameAs(officer);
            assertThat(saved.getFee()).isEqualByComparingTo("50.00");
            assertThat(saved.getStatus()).isEqualTo(RequestStatus.Submitted);
            assertThat(saved.getSubmissionDate()).isEqualTo(LocalDate.now());
            assertThat(saved.getExpectedCompletionDate())
                    .isEqualTo(LocalDate.now().plusDays(7));
            assertThat(response.message()).contains("7 working days");
            verify(notificationService).createNotification(any());
        }

        @Test
        @DisplayName("throws when the service does not exist")
        void serviceNotFound() {
            SubmitServiceRequest req = new SubmitServiceRequest("CIT-1", "SVC-X");
            when(citizenLookup.loadSubmittableCitizen("CIT-1")).thenReturn(citizen);
            when(catalogRepository.findById("SVC-X")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.submitRequest(req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("SVC-X");

            verify(requestRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects submission against an Inactive service")
        void inactiveService() {
            catalog.setStatus(ServiceStatus.Inactive);
            SubmitServiceRequest req = new SubmitServiceRequest("CIT-1", "SVC-1");
            when(citizenLookup.loadSubmittableCitizen("CIT-1")).thenReturn(citizen);
            when(catalogRepository.findById("SVC-1")).thenReturn(Optional.of(catalog));

            assertThatThrownBy(() -> service.submitRequest(req))
                    .isInstanceOf(UnprocessableEntityException.class)
                    .hasMessageContaining("Inactive");

            verify(officerAssignment, never()).findLeastLoadedOfficer(any());
            verify(requestRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAllRequests")
    class GetAllRequests {

        @Test
        @DisplayName("no filters -> findAll")
        void noFilters() {
            when(requestRepository.findAll()).thenReturn(List.of(sampleRequest()));

            List<RequestListItemResponse> result = service.getAllRequests(null, "  ");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).serviceName()).isEqualTo("Birth Certificate");
            assertThat(result.get(0).departmentId()).isEqualTo("DEP-1");
            verify(requestRepository).findAll();
        }

        @Test
        @DisplayName("status only -> findByStatus")
        void statusOnly() {
            when(requestRepository.findByStatus(RequestStatus.Submitted))
                    .thenReturn(List.of(sampleRequest()));

            service.getAllRequests(RequestStatus.Submitted, null);

            verify(requestRepository).findByStatus(RequestStatus.Submitted);
        }

        @Test
        @DisplayName("department only -> findByService_Department_DepartmentId")
        void departmentOnly() {
            when(requestRepository.findByService_Department_DepartmentId("DEP-1"))
                    .thenReturn(List.of());

            service.getAllRequests(null, "DEP-1");

            verify(requestRepository).findByService_Department_DepartmentId("DEP-1");
        }

        @Test
        @DisplayName("both filters -> findByStatusAndService_Department_DepartmentId")
        void bothFilters() {
            when(requestRepository.findByStatusAndService_Department_DepartmentId(
                    RequestStatus.Submitted, "DEP-1")).thenReturn(List.of());

            service.getAllRequests(RequestStatus.Submitted, "DEP-1");

            verify(requestRepository)
                    .findByStatusAndService_Department_DepartmentId(RequestStatus.Submitted, "DEP-1");
        }
    }

    @Nested
    @DisplayName("getRequest")
    class GetRequest {

        @Test
        @DisplayName("returns detail with its documents")
        void returnsDetail() {
            ServiceRequest request = sampleRequest();
            RequestDocument doc = new RequestDocument();
            doc.setDocId("DOC-1");
            doc.setDocumentType("NationalID");
            doc.setVerificationStatus(VerificationStatus.Pending);
            doc.setUploadedDate(LocalDateTime.now());

            when(requestRepository.findById("REQ-1")).thenReturn(Optional.of(request));
            when(documentRepository.findByRequest_RequestId("REQ-1")).thenReturn(List.of(doc));

            RequestDetailResponse detail = service.getRequest("REQ-1");

            assertThat(detail.requestId()).isEqualTo("REQ-1");
            assertThat(detail.serviceName()).isEqualTo("Birth Certificate");
            assertThat(detail.citizenId()).isEqualTo("CIT-1");
            assertThat(detail.feeSnapshot()).isEqualByComparingTo("50.00");
            assertThat(detail.documents()).hasSize(1);
            DocumentItemResponse item = detail.documents().get(0);
            assertThat(item.docId()).isEqualTo("DOC-1");
            assertThat(item.verificationStatus()).isEqualTo(VerificationStatus.Pending);
        }

        @Test
        @DisplayName("throws when the request does not exist")
        void notFound() {
            when(requestRepository.findById("REQ-X")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getRequest("REQ-X"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Test
    @DisplayName("getRequestsByCitizen maps each request to a tracker item")
    void getRequestsByCitizen() {
        when(requestRepository.findByCitizen_CitizenId("CIT-1"))
                .thenReturn(List.of(sampleRequest()));

        List<CitizenRequestItemResponse> result = service.getRequestsByCitizen("CIT-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).requestId()).isEqualTo("REQ-1");
        assertThat(result.get(0).serviceName()).isEqualTo("Birth Certificate");
        assertThat(result.get(0).status()).isEqualTo(RequestStatus.Submitted);
    }

    @Nested
    @DisplayName("updateRequestStatus")
    class UpdateRequestStatus {

        @Test
        @DisplayName("applies a valid transition and saves")
        void validTransition() {
            ServiceRequest request = sampleRequest();
            request.setStatus(RequestStatus.Submitted);
            when(requestRepository.findById("REQ-1")).thenReturn(Optional.of(request));

            MessageResponse response = service.updateRequestStatus(
                    "REQ-1", new UpdateRequestStatusRequest(RequestStatus.UnderReview, "looks good"));

            assertThat(request.getStatus()).isEqualTo(RequestStatus.UnderReview);
            assertThat(response.message()).contains("UnderReview");
            verify(requestRepository).save(request);

            ArgumentCaptor<NotificationRequestDTO> notificationCaptor = ArgumentCaptor.forClass(NotificationRequestDTO.class);
            verify(notificationService).createNotification(notificationCaptor.capture());
            NotificationRequestDTO notification = notificationCaptor.getValue();
            assertThat(notification.userId()).isEqualTo("USR-1");
            assertThat(notification.category()).isEqualTo(Category.ServiceRequest);
            assertThat(notification.message()).contains("now under review");
        }

        @Test
        @DisplayName("rejects updating a terminal request")
        void terminalRequest() {
            ServiceRequest request = sampleRequest();
            request.setStatus(RequestStatus.Completed);
            when(requestRepository.findById("REQ-1")).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.updateRequestStatus(
                    "REQ-1", new UpdateRequestStatusRequest(RequestStatus.UnderReview, null)))
                    .isInstanceOf(UnprocessableEntityException.class)
                    .hasMessageContaining("terminal");

            verify(requestRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects an illegal transition")
        void illegalTransition() {
            ServiceRequest request = sampleRequest();
            request.setStatus(RequestStatus.Submitted);
            when(requestRepository.findById("REQ-1")).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.updateRequestStatus(
                    "REQ-1", new UpdateRequestStatusRequest(RequestStatus.Approved, null)))
                    .isInstanceOf(UnprocessableEntityException.class)
                    .hasMessageContaining("Invalid status transition");

            verify(requestRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when the request does not exist")
        void notFound() {
            when(requestRepository.findById("REQ-X")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateRequestStatus(
                    "REQ-X", new UpdateRequestStatusRequest(RequestStatus.UnderReview, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    private ServiceRequest sampleRequest() {
        ServiceRequest request = new ServiceRequest();
        request.setRequestId("REQ-1");
        request.setCitizen(citizen);
        request.setService(catalog);
        request.setSubmissionDate(LocalDate.now());
        request.setAssignedOfficer(officer);
        request.setFee(new BigDecimal("50.00"));
        request.setExpectedCompletionDate(LocalDate.now().plusDays(7));
        request.setStatus(RequestStatus.Submitted);
        return request;
    }
}
