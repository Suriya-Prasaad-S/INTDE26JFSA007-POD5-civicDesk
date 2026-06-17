package com.civicdesk.module.serviceRequest.controller;

import com.civicdesk.module.serviceRequest.dto.request.CreateServiceRequest;
import com.civicdesk.module.serviceRequest.dto.request.SubmitServiceRequest;
import com.civicdesk.module.serviceRequest.dto.request.UpdateRequestStatusRequest;
import com.civicdesk.module.serviceRequest.dto.request.UpdateServiceRequest;
import com.civicdesk.module.serviceRequest.dto.request.VerifyDocumentRequest;
import com.civicdesk.module.serviceRequest.dto.response.CitizenRequestItemResponse;
import com.civicdesk.module.serviceRequest.dto.response.DocumentItemResponse;
import com.civicdesk.module.serviceRequest.dto.response.MessageResponse;
import com.civicdesk.module.serviceRequest.dto.response.RequestDetailResponse;
import com.civicdesk.module.serviceRequest.dto.response.RequestListItemResponse;
import com.civicdesk.module.serviceRequest.dto.response.ServiceDetailResponse;
import com.civicdesk.module.serviceRequest.dto.response.ServiceListItemResponse;
import com.civicdesk.module.serviceRequest.entity.enums.RequestStatus;
import com.civicdesk.module.serviceRequest.entity.enums.ServiceCategory;
import com.civicdesk.module.serviceRequest.service.DocumentService;
import com.civicdesk.module.serviceRequest.service.ServiceCatalogService;
import com.civicdesk.module.serviceRequest.service.ServiceRequestService;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST endpoints for the Service Request Management module, grouped as: catalog services,
 * service requests, and documents. Mapped at {@code /serviceRequest}; with the application's
 * {@code /civicDesk} context-path the external paths are {@code /civicDesk/serviceRequest/**}.
 *
 * <p>Authentication is owned by the IAM module. These endpoints are currently permitted in
 * {@code SecurityConfig} (role-based authorization on them is a follow-up). Business-rule and
 * data checks (not found, invalid transition, inactive service, bad file type, etc.) are
 * enforced here and now.</p>
 */
@RestController
@RequestMapping("/serviceRequest")
public class ServiceRequestController {

    @Autowired
    private ServiceCatalogService serviceCatalogService;
    @Autowired
    private ServiceRequestService serviceRequestService;
    @Autowired
    private DocumentService documentService;

    // ---------------------------------------------------------------- Catalog services

    /** List all Active services, optionally filtered by category. */
    @GetMapping("/getAllServices")
    public ResponseEntity<List<ServiceListItemResponse>> getAllServices(
            @RequestParam(value = "category", required = false) ServiceCategory category) {
        return ResponseEntity.ok(serviceCatalogService.getAllServices(category));
    }

    /** Full details of one service by its id. */
    @GetMapping("/getService/{serviceId}")
    public ResponseEntity<ServiceDetailResponse> getService(@PathVariable String serviceId) {
        return ResponseEntity.ok(serviceCatalogService.getService(serviceId));
    }

    /** Create a new service in the catalog (Admin only - role check deferred). */
    @PostMapping("/createService")
    public ResponseEntity<MessageResponse> createService(@Valid @RequestBody CreateServiceRequest request) {
        MessageResponse response = serviceCatalogService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Update a service's editable fields or deactivate it (Admin only - role check deferred). */
    @PutMapping("/updateService/{serviceId}")
    public ResponseEntity<MessageResponse> updateService(@PathVariable String serviceId,
                                                         @Valid @RequestBody UpdateServiceRequest request) {
        return ResponseEntity.ok(serviceCatalogService.updateService(serviceId, request));
    }

    // ---------------------------------------------------------------- Service requests

    /** Submit a new service request (Citizen). */
    @PostMapping("/submitRequest")
    public ResponseEntity<MessageResponse> submitRequest(@Valid @RequestBody SubmitServiceRequest request) {
        MessageResponse response = serviceRequestService.submitRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** List service requests, optionally filtered by status and/or department. */
    @GetMapping("/getAllRequests")
    public ResponseEntity<List<RequestListItemResponse>> getAllRequests(
            @RequestParam(value = "status", required = false) RequestStatus status,
            @RequestParam(value = "departmentId", required = false) String departmentId) {
        return ResponseEntity.ok(serviceRequestService.getAllRequests(status, departmentId));
    }

    /** Full details of a single request, including its uploaded documents. */
    @GetMapping("/getRequest/{requestId}")
    public ResponseEntity<RequestDetailResponse> getRequest(@PathVariable String requestId) {
        return ResponseEntity.ok(serviceRequestService.getRequest(requestId));
    }

    /** List all requests submitted by a specific citizen (personal tracker). */
    @GetMapping("/getRequestsByCitizen/{citizenId}")
    public ResponseEntity<List<CitizenRequestItemResponse>> getRequestsByCitizen(
            @PathVariable String citizenId) {
        return ResponseEntity.ok(serviceRequestService.getRequestsByCitizen(citizenId));
    }

    /** Transition a request to the next valid status (Officer/Supervisor - role check deferred). */
    @PutMapping("/updateRequestStatus/{requestId}")
    public ResponseEntity<MessageResponse> updateRequestStatus(
            @PathVariable String requestId,
            @Valid @RequestBody UpdateRequestStatusRequest request) {
        return ResponseEntity.ok(serviceRequestService.updateRequestStatus(requestId, request));
    }

    // ---------------------------------------------------------------- Documents

    /** Upload a supporting document (PDF/JPG/PNG) for a request (Citizen, own request). */
    @PostMapping(path = "/uploadDocument/{requestId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse> uploadDocument(@PathVariable String requestId,
                                                          @RequestParam("documentType") String documentType,
                                                          @RequestParam("file") MultipartFile file) {
        MessageResponse response = documentService.uploadDocument(requestId, documentType, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** List all documents uploaded for a request with their verification statuses. */
    @GetMapping("/getDocuments/{requestId}")
    public ResponseEntity<List<DocumentItemResponse>> getDocuments(@PathVariable String requestId) {
        return ResponseEntity.ok(documentService.getDocuments(requestId));
    }

    /** Officer marks a document Verified or Rejected (role check deferred). */
    @PutMapping("/verifyDocument/{docId}")
    public ResponseEntity<MessageResponse> verifyDocument(@PathVariable String docId,
                                                          @RequestBody VerifyDocumentRequest request) {
        return ResponseEntity.ok(documentService.verifyDocument(docId, request));
    }
}
