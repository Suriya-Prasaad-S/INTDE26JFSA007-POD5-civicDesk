package com.civicdesk.module.serviceRequest.service;

import com.civicdesk.common.exception.BadRequestException;
import com.civicdesk.common.exception.ResourceNotFoundException;
import com.civicdesk.common.exception.UnprocessableEntityException;
import com.civicdesk.module.notification.dto.request.NotificationRequestDTO;
import com.civicdesk.module.notification.entity.enums.Category;
import com.civicdesk.module.notification.service.NotificationService;
import com.civicdesk.module.serviceRequest.dto.request.VerifyDocumentRequest;
import com.civicdesk.module.serviceRequest.dto.response.DocumentItemResponse;
import com.civicdesk.module.serviceRequest.dto.response.MessageResponse;
import com.civicdesk.module.serviceRequest.entity.RequestDocument;
import com.civicdesk.module.serviceRequest.entity.ServiceRequest;
import com.civicdesk.module.serviceRequest.entity.enums.RequestStatus;
import com.civicdesk.module.serviceRequest.entity.enums.VerificationStatus;
import com.civicdesk.module.serviceRequest.repository.RequestDocumentRepository;
import com.civicdesk.module.serviceRequest.repository.ServiceRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for uploading supporting documents against a service request (FR-03).
 * The file is validated and stored on disk; only its path is persisted.
 */
@Service
public class DocumentService {

    private final RequestDocumentRepository documentRepository;
    private final ServiceRequestRepository requestRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    public DocumentService(RequestDocumentRepository documentRepository,
                           ServiceRequestRepository requestRepository,
                           FileStorageService fileStorageService,
                           NotificationService notificationService) {
        this.documentRepository = documentRepository;
        this.requestRepository = requestRepository;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
    }

    @Transactional
    public MessageResponse uploadDocument(String requestId, String documentType, MultipartFile file) {
        if (!StringUtils.hasText(documentType)) {
            throw new BadRequestException("documentType is required");
        }

        // Request must exist and not be in a terminal state.
        ServiceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Request with ID " + requestId + " does not exist"));
        if (request.getStatus().isTerminal()) {
            throw new UnprocessableEntityException(
                    "Cannot upload documents to a Completed or Rejected request.");
        }

        // store() validates the file type (PDF/JPG/PNG) and returns the stored path.
        // The file name carries the citizen id and request id.
        String filePath = fileStorageService.store(file, requestId, request.getCitizen().getCitizenId());

        RequestDocument document = new RequestDocument();
        document.setDocId(UUID.randomUUID().toString());
        document.setRequest(request);
        document.setDocumentType(documentType);
        document.setFilePath(filePath);
        document.setUploadedDate(LocalDateTime.now());
        document.setVerificationStatus(VerificationStatus.Pending);

        documentRepository.save(document);

        String message = "Your document has been uploaded and is pending officer verification.";
        notificationService.createNotification(new NotificationRequestDTO(
                request.getCitizen().getUserId(),
                message,
                Category.ServiceRequest));

        return new MessageResponse(
                "Document uploaded successfully. Document is pending officer verification.");
    }

    /** All documents uploaded against a request, with their verification statuses (getDocuments). */
    @Transactional(readOnly = true)
    public List<DocumentItemResponse> getDocuments(String requestId) {
        if (!requestRepository.existsById(requestId)) {
            throw new ResourceNotFoundException(
                    "Request not found. No request exists with the given requestId.");
        }
        return documentRepository.findByRequest_RequestId(requestId).stream()
                .map(d -> new DocumentItemResponse(
                        d.getDocId(), d.getDocumentType(), d.getVerificationStatus(), d.getUploadedDate()))
                .toList();
    }

    /**
     * Officer verification of a document (verifyDocument). Only Verified / Rejected are
     * accepted. A rejection moves the parent request back to PendingDocuments so the citizen
     * can re-upload.
     */
    @Transactional
    public MessageResponse verifyDocument(String docId, VerifyDocumentRequest request) {
        VerificationStatus newStatus = request.verificationStatus();
        if (newStatus == null || newStatus == VerificationStatus.Pending) {
            throw new BadRequestException(
                    "Validation failed. verificationStatus must be Verified or Rejected.");
        }

        RequestDocument document = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found. No document exists with the given docId."));

        document.setVerificationStatus(newStatus);
        documentRepository.save(document);

        if (newStatus == VerificationStatus.Rejected) {
            ServiceRequest parent = document.getRequest();
            parent.setStatus(RequestStatus.PendingDocuments);
            requestRepository.save(parent);

            String message = "Your document has been rejected. Your service request " + parent.getRequestId()
                    + " requires additional documents. Please upload the requested documents to continue.";
            notificationService.createNotification(new NotificationRequestDTO(
                    parent.getCitizen().getUserId(),
                    message,
                    Category.ServiceRequest));

            return new MessageResponse(
                    "Document rejected. Document status has been set to Rejected. Request has been "
                            + "moved to PendingDocuments. Citizen has been notified to re-upload.");
        }

        String message = "Your document has been verified and accepted.";
        notificationService.createNotification(new NotificationRequestDTO(
                document.getRequest().getCitizen().getUserId(),
                message,
                Category.ServiceRequest));

        return new MessageResponse(
                "Document verified successfully. Document status has been updated to Verified.");
    }
}
