package com.civicdesk.module.citizen.service;

import com.civicdesk.common.util.SecurityContextUtil;
import com.civicdesk.module.citizen.dto.request.VerifyDocumentRequest;
import com.civicdesk.module.citizen.dto.response.DocumentDetailResponse;
import com.civicdesk.module.citizen.dto.response.DocumentSummaryResponse;
import com.civicdesk.module.citizen.entity.CitizenDocument;
import com.civicdesk.module.citizen.entity.enums.DocumentStatus;
import com.civicdesk.module.citizen.entity.enums.DocumentType;
import com.civicdesk.common.exception.citizen.BusinessRuleException;
import com.civicdesk.common.exception.citizen.ForbiddenActionException;
import com.civicdesk.common.exception.citizen.InvalidRequestException;
import com.civicdesk.common.exception.citizen.ResourceNotFoundException;
import com.civicdesk.module.citizen.repository.CitizenDocumentRepository;
import com.civicdesk.module.citizen.repository.CitizenProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business logic for citizen documents.
 *
 * <p>{@code citizenId} on these operations is the citizen's {@code userId}. File bytes are written
 * to disk by {@code FileStorageService} (the controller produces the stored {@code filePath});
 * this service enforces the upload rules and persists the record. {@code documentId} is a
 * 16-character alphanumeric id; {@code status} is exposed on the API as its single-character code.
 *
 * <p>Role gating is handled by {@code @PreAuthorize} on the controller (citizen ops require
 * {@code CIT}; verification requires {@code FO}/{@code DS}/{@code ADM}). Citizen ops additionally
 * enforce that a {@code CIT} caller acts only on their own documents. The verifier's identity is
 * taken from the JWT.
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

    public DocumentService(CitizenDocumentRepository documentRepository,
                           CitizenProfileRepository citizenRepository) {
        this.documentRepository = documentRepository;
        this.citizenRepository = citizenRepository;
    }

    /**
     * Records an uploaded document after enforcing the upload rules: the caller must be the owning
     * citizen (403), the citizen must exist (404), the count must be under
     * {@value #MAX_DOCUMENTS_PER_CITIZEN} (409), the size must be within 2 MB and the type must be
     * PDF/JPG/JPEG/PNG by both extension and MIME (400). The new document starts
     * {@link DocumentStatus#Valid}.
     *
     * @param storedFilePath the retrieval path/URL of the already-stored file (from FileStorageService)
     * @return the generated {@code documentId}
     */
    @Transactional
    public String uploadDocument(String citizenId, String documentType, String originalFileName,
                                 String contentType, long sizeBytes, String storedFilePath) {
        requireSelf(citizenId);
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
        requireSelf(citizenId);
        requireCitizenExists(citizenId);
        return documentRepository.findByCitizenId(citizenId).stream()
                .map(DocumentService::toSummary)
                .toList();
    }

    /** Returns one document scoped to its owning citizen (404 if not found for that citizen). */
    public DocumentDetailResponse getDocumentById(String citizenId, String documentId) {
        requireSelf(citizenId);
        CitizenDocument document = documentRepository
                .findByDocumentIdAndCitizenId(documentId, citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        return toDetail(document);
    }

    /**
     * Verifies a document. The caller (an officer, gated by {@code @PreAuthorize}) is recorded as
     * {@code verifiedBy} from the JWT. Applies the target status (single-char code), enforcing the
     * manual transitions: {@code V->V} (confirm), {@code V->R}, {@code E->R}. {@code E} is never a
     * manual target; {@code R} is terminal. An expired-but-still-{@code V} document is treated as
     * {@code E} for this check.
     */
    @Transactional
    public void verifyDocument(String citizenId, String documentId, VerifyDocumentRequest request) {
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
        document.setVerifiedBy(currentUserId());
        document.setVerifiedAt(LocalDateTime.now());
        documentRepository.save(document);
    }

    /**
     * Resolves the stored file name for a document after authorizing the caller: the owning citizen
     * (CIT) or any officer (FO/DS/ADM). 404 if the document does not exist for that citizen.
     */
    public String resolveDownloadFileName(String citizenId, String documentId) {
        authorizeView(citizenId);
        CitizenDocument document = documentRepository
                .findByDocumentIdAndCitizenId(documentId, citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        return document.getFilePath();
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /** A {@code CIT} caller may act only on their own documents (path id must equal the JWT id). */
    private void requireSelf(String citizenId) {
        if (!citizenId.equals(currentUserId())) {
            throw new ForbiddenActionException("You can only access your own documents");
        }
    }

    /** Document viewing is allowed for the owning citizen or any officer. */
    private void authorizeView(String citizenId) {
        String role = SecurityContextUtil.getCurrentRole();
        if ("CIT".equals(role)) {
            requireSelf(citizenId);
            return;
        }
        if ("FO".equals(role) || "DS".equals(role) || "ADM".equals(role)) {
            return;
        }
        throw new ForbiddenActionException("Not permitted to access this document");
    }

    /** Manual (verify-time) document transitions. Auto-expiry ({@code V->E}) is excluded. */
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

    private static String currentUserId() {
        String userId = SecurityContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new ForbiddenActionException("No authenticated user in the security context");
        }
        return userId;
    }
}
