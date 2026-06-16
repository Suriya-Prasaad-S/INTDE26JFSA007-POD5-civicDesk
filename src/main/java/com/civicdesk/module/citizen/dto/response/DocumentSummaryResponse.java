package com.civicdesk.module.citizen.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Item shape for the list returned by GET /{citizenId}/getAllDocuments.
 * Deliberately omits {@code filePath}, {@code citizenId} and {@code verifiedBy} — those are only
 * exposed by the single-document detail endpoint. {@code status} is the single-character code.
 */
public record DocumentSummaryResponse(
        String documentId,
        String documentType,
        String fileName,
        String fileType,
        Integer fileSizeKb,
        String status,
        LocalDate issuedDate,
        LocalDate expiryDate,
        LocalDateTime verifiedAt,
        LocalDateTime uploadedAt
) {
}
