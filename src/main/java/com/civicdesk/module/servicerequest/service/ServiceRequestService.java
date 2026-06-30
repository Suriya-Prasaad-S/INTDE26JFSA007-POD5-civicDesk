package com.civicdesk.module.serviceRequest.service;

import com.civicdesk.common.exception.ResourceNotFoundException;
import com.civicdesk.common.exception.UnprocessableEntityException;
import com.civicdesk.module.notification.dto.request.NotificationRequestDTO;
import com.civicdesk.module.notification.entity.enums.Category;
import com.civicdesk.module.notification.service.NotificationService;
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
import com.civicdesk.module.serviceRequest.entity.enums.ServiceStatus;
import com.civicdesk.module.serviceRequest.entity.external.CitizenProfile;
import com.civicdesk.module.serviceRequest.entity.external.User;
import com.civicdesk.module.serviceRequest.repository.RequestDocumentRepository;
import com.civicdesk.module.serviceRequest.repository.ServiceCatalogRepository;
import com.civicdesk.module.serviceRequest.repository.ServiceRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for submitting service requests (FR-02).
 *
 * <p>On submission the service: validates the citizen may submit, validates the service
 * is Active, snapshots the fee from the catalog, computes the expected completion date,
 * auto-assigns the least-loaded officer in the service's department, and sets the initial
 * status to {@code Submitted}.</p>
 */
@Service
public class ServiceRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRequestService.class);

    private final ServiceRequestRepository requestRepository;
    private final ServiceCatalogRepository catalogRepository;
    private final RequestDocumentRepository documentRepository;
    private final CitizenLookup citizenLookup;
    private final OfficerAssignment officerAssignment;
    private final NotificationService notificationService;

    public ServiceRequestService(ServiceRequestRepository requestRepository,
                                 ServiceCatalogRepository catalogRepository,
                                 RequestDocumentRepository documentRepository,
                                 CitizenLookup citizenLookup,
                                 OfficerAssignment officerAssignment,
                                 NotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.catalogRepository = catalogRepository;
        this.documentRepository = documentRepository;
        this.citizenLookup = citizenLookup;
        this.officerAssignment = officerAssignment;
        this.notificationService = notificationService;
    }

    @Transactional
    public MessageResponse submitRequest(SubmitServiceRequest request) {
        LOGGER.info("Submitting service request for citizen {} and service {}", request.citizenId(), request.serviceId());

        CitizenProfile citizen = citizenLookup.loadSubmittableCitizen(request.citizenId());

        ServiceCatalog service = catalogRepository.findById(request.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service with ID " + request.serviceId() + " does not exist"));
        if (service.getStatus() != ServiceStatus.Active) {
            throw new UnprocessableEntityException(
                    "The selected service is currently Inactive and not accepting new requests.");
        }

        LocalDate submissionDate = LocalDate.now();
        User officer = officerAssignment.findLeastLoadedOfficer(service.getDepartment().getDepartmentId());

        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setRequestId(UUID.randomUUID().toString());
        serviceRequest.setCitizen(citizen);
        serviceRequest.setService(service);
        serviceRequest.setSubmissionDate(submissionDate);
        serviceRequest.setFee(service.getFee());
        serviceRequest.setExpectedCompletionDate(submissionDate.plusDays(service.getProcessingDays()));
        serviceRequest.setAssignedOfficer(officer);
        serviceRequest.setStatus(RequestStatus.Submitted);

        requestRepository.save(serviceRequest);
        LOGGER.info("Service request {} saved successfully", serviceRequest.getRequestId());

        String message = "Your service request " + serviceRequest.getRequestId()
                + " has been submitted and assigned to an officer. Expected completion date is "
                + service.getProcessingDays() + " working days from today.";

        notificationService.createNotification(new NotificationRequestDTO(
                citizen.getUserId(),
                message,
                Category.ServiceRequest));
        LOGGER.info("Notification created for service request {}", serviceRequest.getRequestId());

        return new MessageResponse(message);
    }

    @Transactional(readOnly = true)
    public List<RequestListItemResponse> getAllRequests(RequestStatus status, String departmentId) {
        boolean hasDept = StringUtils.hasText(departmentId);
        List<ServiceRequest> requests;
        if (status != null && hasDept) {
            requests = requestRepository.findByStatusAndService_Department_DepartmentId(status, departmentId);
        } else if (status != null) {
            requests = requestRepository.findByStatus(status);
        } else if (hasDept) {
            requests = requestRepository.findByService_Department_DepartmentId(departmentId);
        } else {
            requests = requestRepository.findAll();
        }
        return requests.stream().map(this::toListItem).toList();
    }

    @Transactional(readOnly = true)
    public RequestDetailResponse getRequest(String requestId) {
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Request not found. No request exists with the given requestId."));

        List<DocumentItemResponse> documents = documentRepository.findByRequest_RequestId(requestId)
                .stream().map(this::toDocumentItem).toList();

        return new RequestDetailResponse(
                request.getRequestId(),
                request.getService().getServiceName(),
                request.getCitizen().getCitizenId(),
                request.getStatus(),
                request.getFee(),
                request.getExpectedCompletionDate(),
                documents);
    }

    @Transactional(readOnly = true)
    public List<CitizenRequestItemResponse> getRequestsByCitizen(String citizenId) {
        return requestRepository.findByCitizen_CitizenId(citizenId).stream()
                .map(r -> new CitizenRequestItemResponse(
                        r.getRequestId(),
                        r.getService().getServiceName(),
                        r.getStatus(),
                        r.getExpectedCompletionDate()))
                .toList();
    }

    @Transactional
    public MessageResponse updateRequestStatus(String requestId, UpdateRequestStatusRequest request) {
        ServiceRequest serviceRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Request not found. No request exists with the given requestId."));

        RequestStatus current = serviceRequest.getStatus();
        RequestStatus next = request.newStatus();

        if (current.isTerminal()) {
            throw new UnprocessableEntityException(
                    "Request is in a terminal state. Rejected and completed requests cannot be updated further.");
        }
        if (!current.allowedNextStates().contains(next)) {
            String allowed = current.allowedNextStates().stream()
                    .map(Enum::name).collect(Collectors.joining(", "));
            throw new UnprocessableEntityException(
                    "Invalid status transition. Cannot move from " + current + " to " + next
                            + ". Allowed next states: " + allowed + ".");
        }

        serviceRequest.setStatus(next);
        requestRepository.save(serviceRequest);
        LOGGER.info("Request {} status updated to {}", requestId, next);

        String notificationMessage = buildStatusNotificationMessage(requestId, next);
        notificationService.createNotification(new NotificationRequestDTO(
                serviceRequest.getCitizen().getUserId(),
                notificationMessage,
                Category.ServiceRequest));
        LOGGER.info("Notification created for request {} after status update", requestId);

        return new MessageResponse(
                "Request status updated successfully. Status has been moved to " + next + ".");
    }

    private String buildStatusNotificationMessage(String requestId, RequestStatus next) {
        return switch (next) {
            case UnderReview -> "Your service request " + requestId + " is now under review.";
            case PendingDocuments -> "Your service request " + requestId + " requires additional documents. Please upload the requested documents to continue.";
            case Approved -> "Your service request " + requestId + " has been approved.";
            case Rejected -> "Your service request " + requestId + " has been rejected.";
            case Completed -> "Your service request " + requestId + " has been completed.";
            default -> "Your service request " + requestId + " status has been updated to " + next + ".";
        };
    }

    private RequestListItemResponse toListItem(ServiceRequest r) {
        return new RequestListItemResponse(
                r.getRequestId(),
                r.getService().getServiceName(),
                r.getCitizen().getCitizenId(),
                r.getStatus(),
                r.getService().getDepartment().getDepartmentId(),
                r.getExpectedCompletionDate());
    }

    private DocumentItemResponse toDocumentItem(RequestDocument d) {
        return new DocumentItemResponse(
                d.getDocId(), d.getDocumentType(), d.getVerificationStatus(), d.getUploadedDate());
    }
}
