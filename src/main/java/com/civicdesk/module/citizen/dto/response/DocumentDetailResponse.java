package com.civicdesk.module.citizen.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response payload for GET /{citizenId}/getDocumentById/{documentId} — the full document detail,
 * including {@code filePath} (the retrieval URL) and verification metadata. {@code status} is the
 * single-character code.
 */
public record DocumentDetailResponse(
        String documentId,
        String citizenId,
        String documentType,
        String fileName,
        String filePath,
        String fileType,
        Integer fileSizeKb,
        LocalDate issuedDate,
        LocalDate expiryDate,
        String status,
        String verifiedBy,
        LocalDateTime verifiedAt,
        LocalDateTime uploadedAt
) {
}
