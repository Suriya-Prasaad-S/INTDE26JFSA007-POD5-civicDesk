package com.civicdesk.module.citizen.entity;

import com.civicdesk.module.citizen.entity.converter.DocumentStatusConverter;
import com.civicdesk.module.citizen.entity.enums.DocumentStatus;
import com.civicdesk.module.citizen.entity.enums.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A document belonging to a citizen. Maps to the {@code citizen_document} table (snake_case).
 *
 * <p>{@code document_id} and {@code citizen_id} are 16-character alphanumeric ids (the citizen id is
 * a plain reference, not a JPA relationship, to keep this module decoupled). The actual file is
 * stored on disk by {@code FileStorageService}; {@code file_path} holds the retrieval URL. The
 * on-disk name is generated (never the user's name) to prevent path traversal. {@code status}
 * persists as a single-character code (V/E/R) via {@link DocumentStatusConverter}.
 */
@Entity
@Table(
        name = "citizen_document",
        // Backs findByCitizenId / countByCitizenId / findByDocumentIdAndCitizenId.
        indexes = @Index(name = "idx_citizen_document_citizen_id", columnList = "citizen_id")
)
public class CitizenDocument {

    @Id
    @Column(name = "document_id", length = 16, nullable = false, updatable = false)
    private String documentId;

    @Column(name = "citizen_id", length = 16, nullable = false)
    private String citizenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    @Check(constraints = "document_type in ('NationalID','ResidenceProof','BirthCertificate','IncomeCertificate')")
    private DocumentType documentType;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path", length = 512)
    private String filePath;

    // Short file extension only — one of pdf / jpg / jpeg / png, lowercased by the service.
    @Column(name = "file_type", length = 10)
    private String fileType;

    @Column(name = "file_size_kb")
    private Integer fileSizeKb;

    @Column(name = "issued_date")
    private LocalDate issuedDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Convert(converter = DocumentStatusConverter.class)
    @Column(name = "status", nullable = false, length = 1)
    @Check(constraints = "status in ('V','E','R')")
    private DocumentStatus status;

    // References users.userId (IAM, CHAR(36)); kept at length 36, not the citizen module's 16.
    @Column(name = "verified_by", length = 36)
    private String verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getCitizenId() {
        return citizenId;
    }

    public void setCitizenId(String citizenId) {
        this.citizenId = citizenId;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Integer getFileSizeKb() {
        return fileSizeKb;
    }

    public void setFileSizeKb(Integer fileSizeKb) {
        this.fileSizeKb = fileSizeKb;
    }

    public LocalDate getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(LocalDate issuedDate) {
        this.issuedDate = issuedDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
}
