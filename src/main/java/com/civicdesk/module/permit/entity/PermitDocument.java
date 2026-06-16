package com.civicdesk.module.permit.entity;

import com.civicdesk.module.permit.enums.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "permit_document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermitDocument {

    @Id
    @Column(name = "documentId", length = 36)
    private String documentId;

    @Column(name = "permitId", nullable = false, length = 36)
    private String permitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "documentType", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(name = "filePath", nullable = false, length = 255)
    private String filePath;

    @Column(name = "uploadedAt", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "verificationStatus", nullable = false, length = 20)
    private String verificationStatus;

    @Column(name = "verificationRemarks", length = 255)
    private String verificationRemarks;

    @Column(name = "isDeleted", nullable = false)
    private boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        uploadedAt = LocalDateTime.now();
        verificationStatus = "Pending";
    }
}