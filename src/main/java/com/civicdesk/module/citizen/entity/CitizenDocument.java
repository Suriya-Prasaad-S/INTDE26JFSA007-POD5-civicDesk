package com.civicdesk.module.citizen.entity;

import com.civicdesk.common.id.NumericStringSequenceGenerator;
import com.civicdesk.module.citizen.entity.converter.DocumentStatusConverter;
import com.civicdesk.module.citizen.entity.enums.DocumentStatus;
import com.civicdesk.module.citizen.entity.enums.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A document belonging to a citizen. Maps to the {@code citizen_document} table.
 *
 * <p>{@code documentId} is a sequential numeric id rendered as a String (matching IAM's id
 * strategy). {@code citizenId} is a plain reference to the owning citizen's {@code userId} (IAM
 * {@code User}, length 36) — not a JPA relationship, to keep this module decoupled. The actual
 * file is stored on disk by {@code FileStorageService}; {@code filePath} holds the retrieval URL.
 * The on-disk name is generated (never the user's name) to prevent path traversal. {@code status}
 * persists as a single-character code (V/E/R) via {@link DocumentStatusConverter}.
 *
 * <p>Convention: the table name is snake_case ({@code citizen_document}) while column names are
 * camelCase, matching IAM and grievance.
 */
@Entity
@Table(
        name = "citizen_document",
        // Backs findByCitizenId / countByCitizenId / findByDocumentIdAndCitizenId.
        indexes = @Index(name = "idx_citizen_document_citizen_id", columnList = "citizenId")
)
public class CitizenDocument {

    // Sequential numeric id rendered as a String (e.g. 50000001), matching IAM's id strategy.
    @Id
    @GeneratedValue(generator = "citizenDocumentIdSeq")
    @GenericGenerator(
            name = "citizenDocumentIdSeq",
            type = NumericStringSequenceGenerator.class,
            parameters = {
                @Parameter(name = "sequence_name", value = "citizen_document_id_seq"),
                @Parameter(name = "initial_value", value = "50000001"),
                @Parameter(name = "increment_size", value = "1"),
                @Parameter(name = "optimizer", value = "none")
            })
    @Column(name = "documentId", length = 36, nullable = false, updatable = false)
    private String documentId;

    /** The owning citizen's userId (IAM User, CHAR(36)). */
    @Column(name = "citizenId", length = 36, nullable = false)
    private String citizenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "documentType", nullable = false, length = 20)
    @Check(constraints = "documentType in ('NationalID','ResidenceProof','BirthCertificate','IncomeCertificate')")
    private DocumentType documentType;

    @Column(name = "fileName")
    private String fileName;

    @Column(name = "filePath", length = 512)
    private String filePath;

    // Short file extension only — one of pdf / jpg / jpeg / png, lowercased by the service.
    @Column(name = "fileType", length = 10)
    private String fileType;

    @Column(name = "fileSizeKb")
    private Integer fileSizeKb;

    @Column(name = "issuedDate")
    private LocalDate issuedDate;

    @Column(name = "expiryDate")
    private LocalDate expiryDate;

    @Convert(converter = DocumentStatusConverter.class)
    @Column(name = "status", nullable = false, length = 1)
    @Check(constraints = "status in ('V','E','R')")
    private DocumentStatus status;

    /** The officer's userId who verified this document (IAM User, CHAR(36)). */
    @Column(name = "verifiedBy", length = 36)
    private String verifiedBy;

    @Column(name = "verifiedAt")
    private LocalDateTime verifiedAt;

    @CreationTimestamp
    @Column(name = "uploadedAt", updatable = false)
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
