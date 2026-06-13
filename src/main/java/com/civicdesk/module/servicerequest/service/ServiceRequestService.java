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
import com.civicdesk.module.serviceRequest.entity.enums.ServiceStatus;
import com.civicdesk.module.serviceRequest.entity.external.CitizenProfile;
import com.civicdesk.module.serviceRequest.entity.external.User;
import com.civicdesk.module.serviceRequest.repository.RequestDocumentRepository;
import com.civicdesk.module.serviceRequest.repository.ServiceCatalogRepository;
import com.civicdesk.module.serviceRequest.repository.ServiceRequestRepository;
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

    private final ServiceRequestRepository requestRepository;
    private final ServiceCatalogRepository catalogRepository;
    private final RequestDocumentRepository documentRepository;
    private final CitizenLookup citizenLookup;
    private final OfficerAssignment officerAssignment;

    public ServiceRequestService(ServiceRequestRepository requestRepository,
                                 ServiceCatalogRepository catalogRepository,
                                 RequestDocumentRepository documentRepository,
                                 CitizenLookup citizenLookup,
                                 OfficerAssignment officerAssignment) {
        this.requestRepository = requestRepository;
        this.catalogRepository = catalogRepository;
        this.documentRepository = documentRepository;
        this.citizenLookup = citizenLookup;
        this.officerAssignment = officerAssignment;
    }

    @Transactional
    public MessageResponse submitRequest(SubmitServiceRequest request) {
        // 1. Citizen must exist and not be flagged.
        CitizenProfile citizen = citizenLookup.loadSubmittableCitizen(request.citizenId());

        // 2. Service must exist and be Active.
        ServiceCatalog service = catalogRepository.findById(request.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service with ID " + request.serviceId() + " does not exist"));
        if (service.getStatus() != ServiceStatus.Active) {
            throw new UnprocessableEntityException(
                    "The selected service is currently Inactive and not accepting new requests.");
        }

        // 3. Snapshot fee + compute completion date + auto-assign officer.
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

        String message = "Service request submitted successfully. Your request has been received "
                + "and assigned to an officer. Expected completion date is "
                + service.getProcessingDays() + " working days from today.";

        return new MessageResponse(message);
    }

    /** The request queue, optionally filtered by status and/or department (getAllRequests). */
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

    /** Full details of one request including its uploaded documents (getRequest). */
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

    /** All requests submitted by one citizen, for their personal tracker (getRequestsByCitizen). */
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

    /** Transition a request to the next valid status, enforcing the workflow (updateRequestStatus). */
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

        return new MessageResponse(
                "Request status updated successfully. Status has been moved to " + next + ".");
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
