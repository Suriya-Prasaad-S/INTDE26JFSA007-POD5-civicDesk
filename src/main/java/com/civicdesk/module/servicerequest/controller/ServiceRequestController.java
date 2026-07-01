package com.civicdesk.module.serviceRequest.controller;

import com.civicdesk.module.serviceRequest.dto.request.CreateServiceRequest;
import com.civicdesk.module.serviceRequest.dto.request.SubmitServiceRequest;
import com.civicdesk.module.serviceRequest.dto.request.UpdateRequestStatusRequest;
import com.civicdesk.module.serviceRequest.dto.request.UpdateServiceRequest;
import com.civicdesk.module.serviceRequest.dto.request.ServiceRequestAnalyticsRequest;
import com.civicdesk.module.serviceRequest.dto.request.VerifyDocumentRequest;
import com.civicdesk.module.serviceRequest.dto.response.CitizenRequestItemResponse;
import com.civicdesk.module.serviceRequest.dto.response.DocumentItemResponse;
import com.civicdesk.module.serviceRequest.dto.response.MessageResponse;
import com.civicdesk.module.serviceRequest.dto.response.RequestDetailResponse;
import com.civicdesk.module.serviceRequest.dto.response.RequestListItemResponse;
import com.civicdesk.module.serviceRequest.dto.response.ServiceDetailResponse;
import com.civicdesk.module.serviceRequest.dto.response.ServiceListItemResponse;
import com.civicdesk.module.serviceRequest.dto.response.ServiceRequestAnalyticsResponse;
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


@RestController
@RequestMapping("/civicDesk/serviceRequest")
public class ServiceRequestController {

    @Autowired
    private ServiceCatalogService serviceCatalogService;
    @Autowired
    private ServiceRequestService serviceRequestService;
    @Autowired
    private DocumentService documentService;
     public ServiceRequestController(ServiceCatalogService serviceCatalogService,
            ServiceRequestService serviceRequestService, DocumentService documentService) {
        this.serviceCatalogService = serviceCatalogService;
        this.serviceRequestService = serviceRequestService;
        this.documentService = documentService;
    }

    //Catalog services


    @GetMapping("/getAllServices")
    public ResponseEntity<List<ServiceListItemResponse>> getAllServices(
            @RequestParam(value = "category", required = false) ServiceCategory category) {
        return ResponseEntity.ok(serviceCatalogService.getAllServices(category));
    }

    @GetMapping("/getService/{serviceId}")
    public ResponseEntity<ServiceDetailResponse> getService(@PathVariable String serviceId) {
        return ResponseEntity.ok(serviceCatalogService.getService(serviceId));
    }

    @PostMapping("/createService")
    public ResponseEntity<MessageResponse> createService(@Valid @RequestBody CreateServiceRequest request) {
        MessageResponse response = serviceCatalogService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/updateService/{serviceId}")
    public ResponseEntity<MessageResponse> updateService(@PathVariable String serviceId,
                                                         @Valid @RequestBody UpdateServiceRequest request) {
        return ResponseEntity.ok(serviceCatalogService.updateService(serviceId, request));
    }

    //Service requests

    @PostMapping("/submitRequest")
    public ResponseEntity<MessageResponse> submitRequest(@Valid @RequestBody SubmitServiceRequest request) {
        MessageResponse response = serviceRequestService.submitRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/getAllRequests")
    public ResponseEntity<List<RequestListItemResponse>> getAllRequests(
            @RequestParam(value = "status", required = false) RequestStatus status,
            @RequestParam(value = "departmentId", required = false) String departmentId) {
        return ResponseEntity.ok(serviceRequestService.getAllRequests(status, departmentId));
    }

    @PostMapping("/getServiceRequestAnalytics")
    public ResponseEntity<ServiceRequestAnalyticsResponse> getServiceRequestAnalytics(
            @RequestBody ServiceRequestAnalyticsRequest request) {
        return ResponseEntity.ok(serviceRequestService.getServiceRequestAnalytics(request));
    }

    @GetMapping("/getRequest/{requestId}")
    public ResponseEntity<RequestDetailResponse> getRequest(@PathVariable String requestId) {
        return ResponseEntity.ok(serviceRequestService.getRequest(requestId));
    }

    @GetMapping("/getRequestsByCitizen/{citizenId}")
    public ResponseEntity<List<CitizenRequestItemResponse>> getRequestsByCitizen(
            @PathVariable String citizenId) {
        return ResponseEntity.ok(serviceRequestService.getRequestsByCitizen(citizenId));
    }

    @PutMapping("/updateRequestStatus/{requestId}")
    public ResponseEntity<MessageResponse> updateRequestStatus(
            @PathVariable String requestId,
            @Valid @RequestBody UpdateRequestStatusRequest request) {
        return ResponseEntity.ok(serviceRequestService.updateRequestStatus(requestId, request));
    }

    //Documents

   
    @PostMapping(path = "/uploadDocument/{requestId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse> uploadDocument(@PathVariable String requestId,
                                                          @RequestParam("documentType") String documentType,
                                                          @RequestParam("file") MultipartFile file) {
        MessageResponse response = documentService.uploadDocument(requestId, documentType, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    
    @GetMapping("/getDocuments/{requestId}")
    public ResponseEntity<List<DocumentItemResponse>> getDocuments(@PathVariable String requestId) {
        return ResponseEntity.ok(documentService.getDocuments(requestId));
    }

    
    @PutMapping("/verifyDocument/{docId}")
    public ResponseEntity<MessageResponse> verifyDocument(@PathVariable String docId,
                                                          @RequestBody VerifyDocumentRequest request) {
        return ResponseEntity.ok(documentService.verifyDocument(docId, request));
    }
}
