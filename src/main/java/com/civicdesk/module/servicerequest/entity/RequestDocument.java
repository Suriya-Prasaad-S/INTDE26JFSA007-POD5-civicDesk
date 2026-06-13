package com.civicdesk.module.serviceRequest.entity;

import com.civicdesk.module.serviceRequest.entity.enums.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * A supporting document uploaded by a citizen against a {@link ServiceRequest}.
 * Files are stored on the local filesystem; only the path is persisted (Phase 1).
 */
@Entity
@Table(name = "request_document")
public class RequestDocument {

    @Id
    @Column(length = 50)
    private String docId;

    /** The request this document supports. FK column {@code requestId} → {@code service_request}. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "requestId", nullable = false)
    private ServiceRequest request;

    @Column(nullable = false, length = 100)
    private String documentType;

    @Column(nullable = false, length = 500)
    private String filePath;

    @Column(nullable = false)
    private LocalDateTime uploadedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VerificationStatus verificationStatus = VerificationStatus.Pending;

    public RequestDocument() {
        // JPA + service-layer construction
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public ServiceRequest getRequest() {
        return request;
    }

    public void setRequest(ServiceRequest request) {
        this.request = request;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public LocalDateTime getUploadedDate() {
        return uploadedDate;
    }

    public void setUploadedDate(LocalDateTime uploadedDate) {
        this.uploadedDate = uploadedDate;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
}
