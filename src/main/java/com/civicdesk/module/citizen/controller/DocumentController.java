package com.civicdesk.module.citizen.controller;

import com.civicdesk.module.citizen.dto.request.VerifyDocumentRequest;
import com.civicdesk.module.citizen.dto.response.DocumentDetailResponse;
import com.civicdesk.module.citizen.dto.response.DocumentSummaryResponse;
import com.civicdesk.module.citizen.exception.InvalidRequestException;
import com.civicdesk.module.citizen.service.DocumentService;
import com.civicdesk.module.citizen.support.FileStorageService;
import com.civicdesk.module.citizen.support.IdGenerator;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Citizen document endpoints under base path {@code /citizenProfile} (served below the
 * application context path {@code /civicDesk}).
 *
 * <p>{@code uploadDocument} accepts a real {@code multipart/form-data} file: the bytes are written to
 * disk by {@link FileStorageService} under a generated name, {@code filePath} is set to the
 * retrieval URL, and the bytes are served back by {@link #downloadFile}. {@code status} values on the
 * API are single-character codes (V/E/R). The internal "issue dummy document" path has been removed
 * (it belonged to Module 2.3).
 */
@RestController
@RequestMapping("/citizenProfile")
public class DocumentController {

    private final DocumentService documentService;
    private final FileStorageService fileStorage;

    /** Base URL used to build a stored document's retrieval path (matches {@link #downloadFile}). */
    private final String fileBaseUrl;

    public DocumentController(
            DocumentService documentService,
            FileStorageService fileStorage,
            @Value("${citizen.document.base-url:http://localhost:8081/civicDesk/citizenProfile/files}")
            String fileBaseUrl) {
        this.documentService = documentService;
        this.fileStorage = fileStorage;
        this.fileBaseUrl = fileBaseUrl;
    }

    /**
     * #5 — POST /{citizenId}/uploadDocument (multipart/form-data). Form parts: {@code file} (the
     * upload) and {@code documentType}. The file is stored on disk; size/type/limit rules are
     * enforced by the service (and the stored file is rolled back if the service rejects it).
     */
    @PostMapping(value = "/{citizenId}/uploadDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @PathVariable String citizenId,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file) {
        String ext = extensionOf(file.getOriginalFilename());
        String storedName = ext.isEmpty() ? IdGenerator.newId() : IdGenerator.newId() + "." + ext;

        try (InputStream in = file.getInputStream()) {
            fileStorage.store(in, storedName);
        } catch (IOException e) {
            throw new InvalidRequestException("Could not read the uploaded file");
        }

        String storedFilePath = trimTrailingSlash(fileBaseUrl) + "/" + storedName;
        try {
            String documentId = documentService.uploadDocument(
                    citizenId,
                    documentType,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    storedFilePath);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "Document uploaded successfully");
            body.put("documentId", documentId);
            return ResponseEntity.status(201).body(body);
        } catch (RuntimeException ex) {
            fileStorage.deleteQuietly(storedName); // roll back the stored file on validation/persist failure
            throw ex;
        }
    }

    /** #6 — GET /{citizenId}/getAllDocuments. 404 if the citizen does not exist. */
    @GetMapping("/{citizenId}/getAllDocuments")
    public ResponseEntity<List<DocumentSummaryResponse>> getAllDocuments(@PathVariable String citizenId) {
        return ResponseEntity.ok(documentService.getAllDocuments(citizenId));
    }

    /** #7 — GET /{citizenId}/getDocumentById/{documentId}. Scoped to the owning citizen. */
    @GetMapping("/{citizenId}/getDocumentById/{documentId}")
    public ResponseEntity<DocumentDetailResponse> getDocumentById(
            @PathVariable String citizenId,
            @PathVariable String documentId) {
        return ResponseEntity.ok(documentService.getDocumentById(citizenId, documentId));
    }

    /**
     * #8 — PUT /{citizenId}/verifyDocument/{documentId}. The verifier ({@code verifiedBy}) must be an
     * Active Department Supervisor (403 otherwise); applies the manual status transition.
     */
    @PutMapping("/{citizenId}/verifyDocument/{documentId}")
    public ResponseEntity<Map<String, Object>> verifyDocument(
            @PathVariable String citizenId,
            @PathVariable String documentId,
            @Valid @RequestBody VerifyDocumentRequest request) {
        documentService.verifyDocument(citizenId, documentId, request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Document verified successfully");
        return ResponseEntity.ok(body);
    }

    /** GET /files/{filename} — streams a stored document's bytes (used by {@code filePath}). */
    @GetMapping("/files/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        Resource resource = fileStorage.load(filename);
        return ResponseEntity.ok()
                .contentType(contentTypeFor(extensionOf(filename)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    // ------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------

    private static String trimTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
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

    private static MediaType contentTypeFor(String ext) {
        return switch (ext) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
