package com.civicdesk.module.permit.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DocumentResponse {
    private String        documentId;
    private String        documentType;
    private String        filePath;    
    private String        verificationStatus;
    private String        verificationRemarks;
    private LocalDateTime uploadedAt;
}