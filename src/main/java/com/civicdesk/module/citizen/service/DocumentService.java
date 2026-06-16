package com.civicdesk.module.citizen.service;

import com.civicdesk.module.citizen.dto.request.VerifyDocumentRequest;
import com.civicdesk.module.citizen.dto.response.DocumentDetailResponse;
import com.civicdesk.module.citizen.dto.response.DocumentSummaryResponse;
import com.civicdesk.module.citizen.entity.CitizenDocument;
import com.civicdesk.module.citizen.entity.enums.DocumentStatus;
import com.civicdesk.module.citizen.entity.enums.DocumentType;
import com.civicdesk.module.citizen.exception.BusinessRuleException;
import com.civicdesk.module.citizen.exception.ForbiddenActionException;
import com.civicdesk.module.citizen.exception.InvalidRequestException;
import com.civicdesk.module.citizen.exception.ResourceNotFoundException;
import com.civicdesk.module.citizen.repository.CitizenDocumentRepository;
import com.civicdesk.module.citizen.repository.CitizenProfileRepository;
import com.civicdesk.module.citizen.support.IdGenerator;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business logic for citizen documents.
 *
 * <p>The actual file bytes are written to disk by {@code FileStorageService} (the controller
 * produces the stored {@code filePath}); this service enforces the upload rules and persists the
 * record. {@code documentId} is a 16-character alphanumeric id; {@code status} is exposed on the API
 * as its single-character code.
 *
 * <p><b>Verification is gated to a Department Supervisor.</b> Until Module 2.1 (IAM) provides JWT
 * role checks, {@link #verifyDocument} looks up the supplied {@code verifiedBy} in the {@code users}
 * table and rejects (403) anyone who is not an Active {@code DEPT_SUPERVISOR}.
 */
@Service
@Transactional(readOnly = true)
public class DocumentService {

    /** Max documents a single citizen may hold. */
    static final int MAX_DOCUMENTS_PER_CITIZEN = 5;
    /** Max upload size: 2 MB. */
    static final long MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024;
    /** Allowed file extensions (lowercased), persisted into {@code fileType}. */
    static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    /** Allowed MIME types for the upload content-type check. */
    static final Set<String> ALLOWED_MIME_TYPES =
            Set.of("application/pdf", "image/jpeg", "image/png");

    private final CitizenDocumentRepository documentRepository;
    private final CitizenProfileRepository citizenRepository;
    private final JdbcTemplate jdbcTemplate;

    public DocumentService(CitizenDocumentRepository documentRepository,
                           CitizenProfileRepository citizenRepository,
                           JdbcTemplate jdbcTemplate) {
        this.documentRepository = documentRepository;
        this.citizenRepository = citizenRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Records an uploaded document after enforcing the upload rules: the citizen must exist (404),
     * the count must be under {@value #MAX_DOCUMENTS_PER_CITIZEN} (409), the size must be within
     * 2 MB and the type must be PDF/JPG/JPEG/PNG by both extension and MIME (400). The new document
     * starts {@link DocumentStatus#Valid}.
     *
     * @param storedFilePath the retrieval path/URL of the already-stored file (from FileStorageService)
     * @return the generated {@code documentId}
     */
    @Transactional
    public String uploadDocument(String citizenId, String documentType, String originalFileName,
                                 String contentType, long sizeBytes, String storedFilePath) {
        requireCitizenExists(citizenId);
        DocumentType type = parseEnum(DocumentType.class, documentType, "documentType");

        if (documentRepository.countByCitizenId(citizenId) >= MAX_DOCUMENTS_PER_CITIZEN) {
            throw new BusinessRuleException(
                    "Document limit reached (max " + MAX_DOCUMENTS_PER_CITIZEN
                            + ") for citizen " + citizenId);
        }
        if (sizeBytes <= 0) {
            throw new InvalidRequestException("Uploaded file is empty");
        }
        if (sizeBytes > MAX_FILE_SIZE_BYTES) {
            throw new InvalidRequestException("File exceeds the 2 MB limit");
        }
        String extension = extensionOf(originalFileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidRequestException(
                    "Unsupported file type: '." + extension + "'. Allowed: pdf, jpg, jpeg, png");
        }
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidRequestException("Unsupported content type: " + contentType);
        }

        CitizenDocument document = new CitizenDocument();
        document.setDocumentId(IdGenerator.newId());
        document.setCitizenId(citizenId);
        document.setDocumentType(type);
        document.setFileName(originalFileName);
        document.setFilePath(storedFilePath);
        document.setFileType(extension);
        document.setFileSizeKb((int) Math.ceil(sizeBytes / 1024.0));
        document.setStatus(DocumentStatus.Valid);

        documentRepository.save(document);
        return document.getDocumentId();
    }

    /** Lists every document for a citizen (404 if the citizen does not exist). */
    public List<DocumentSummaryResponse> getAllDocuments(String citizenId) {
        requireCitizenExists(citizenId);
        return documentRepository.findByCitizenId(citizenId).stream()
                .map(DocumentService::toSummary)
                .toList();
    }

    /** Returns one document scoped to its owning citizen (404 if not found for that citizen). */
    public DocumentDetailResponse getDocumentById(String citizenId, String documentId) {
        CitizenDocument document = documentRepository
                .findByDocumentIdAndCitizenId(documentId, citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        return toDetail(document);
    }

    /**
     * Verifies a document. The verifier must be an Active {@code DEPT_SUPERVISOR} (403 otherwise).
     * Records {@code verifiedBy}/{@code verifiedAt} and applies the target status (single-char code),
     * enforcing the manual transitions: {@code V&rarr;V} (confirm), {@code V&rarr;R}, {@code E&rarr;R}.
     * {@code E} is never a manual target; {@code R} is terminal. An expired-but-still-{@code V}
     * document is treated as {@code E} for this check.
     */
    @Transactional
    public void verifyDocument(String citizenId, String documentId, VerifyDocumentRequest request) {
        requireDepartmentSupervisor(request.verifiedBy());

        CitizenDocument document = documentRepository
                .findByDocumentIdAndCitizenId(documentId, citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

        DocumentStatus target = parseStatus(request.status());
        DocumentStatus current = effectiveStatus(document);
        if (!isAllowedTransition(current, target)) {
            throw new BusinessRuleException(
                    "Illegal document status transition: " + current.getCode() + " -> " + target.getCode());
        }
        document.setStatus(target);
        document.setVerifiedBy(request.verifiedBy());
        document.setVerifiedAt(LocalDateTime.now());
        documentRepository.save(document);
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /**
     * Stand-in for the Tier-3 JWT role check: the {@code verifiedBy} user must exist in the
     * {@code users} table as an Active {@code DEPT_SUPERVISOR}, else 403.
     */
    private void requireDepartmentSupervisor(String userId) {
        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList(
                    "SELECT role, status FROM users WHERE userId = ?", userId);
        } catch (DataAccessException e) {
            throw new ForbiddenActionException(
                    "Cannot verify the supervisor role (users reference data unavailable)");
        }
        if (rows.isEmpty()) {
            throw new ForbiddenActionException("Unknown verifier: " + userId);
        }
        Map<String, Object> user = rows.get(0);
        if (!"DEPT_SUPERVISOR".equals(String.valueOf(user.get("role")))) {
            throw new ForbiddenActionException("Only a Department Supervisor may verify documents");
        }
        if (!"Active".equals(String.valueOf(user.get("status")))) {
            throw new ForbiddenActionException("Verifier account is not active: " + userId);
        }
    }

    /** Manual (verify-time) document transitions. Auto-expiry ({@code V&rarr;E}) is excluded. */
    private static boolean isAllowedTransition(DocumentStatus from, DocumentStatus to) {
        if (to == DocumentStatus.Expired) {
            return false; // reached automatically, never set by hand
        }
        return switch (from) {
            case Valid -> to == DocumentStatus.Valid || to == DocumentStatus.Revoked;
            case Expired -> to == DocumentStatus.Revoked;
            case Revoked -> false; // terminal
        };
    }

    /** A still-{@code Valid} document past its {@code expiryDate} reads as {@code Expired}. */
    private static DocumentStatus effectiveStatus(CitizenDocument d) {
        if (d.getStatus() == DocumentStatus.Valid
                && d.getExpiryDate() != null
                && d.getExpiryDate().isBefore(LocalDate.now())) {
            return DocumentStatus.Expired;
        }
        return d.getStatus();
    }

    private void requireCitizenExists(String citizenId) {
        if (!citizenRepository.existsById(citizenId)) {
            throw new ResourceNotFoundException("Citizen not found: " + citizenId);
        }
    }

    /** Lowercased extension without the dot, or "" if none. */
    private static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase();
    }

    private static DocumentSummaryResponse toSummary(CitizenDocument d) {
        return new DocumentSummaryResponse(
                d.getDocumentId(),
                d.getDocumentType().name(),
                d.getFileName(),
                d.getFileType(),
                d.getFileSizeKb(),
                effectiveStatus(d).getCode(),
                d.getIssuedDate(),
                d.getExpiryDate(),
                d.getVerifiedAt(),
                d.getUploadedAt());
    }

    private static DocumentDetailResponse toDetail(CitizenDocument d) {
        return new DocumentDetailResponse(
                d.getDocumentId(),
                d.getCitizenId(),
                d.getDocumentType().name(),
                d.getFileName(),
                d.getFilePath(),
                d.getFileType(),
                d.getFileSizeKb(),
                d.getIssuedDate(),
                d.getExpiryDate(),
                effectiveStatus(d).getCode(),
                d.getVerifiedBy(),
                d.getVerifiedAt(),
                d.getUploadedAt());
    }

    /** Parses a single-character status code, raising a precise 400 listing the allowed codes. */
    private static DocumentStatus parseStatus(String value) {
        try {
            return DocumentStatus.fromCode(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidRequestException(
                    "Invalid status: '" + value + "'. Allowed codes: " + DocumentStatus.allowedCodes());
        }
    }

    /** Parses a String into an enum constant, raising a precise 400 listing the allowed values. */
    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException | NullPointerException e) {
            String allowed = Arrays.stream(type.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new InvalidRequestException(
                    "Invalid " + field + ": '" + value + "'. Allowed values: " + allowed);
        }
    }
}
