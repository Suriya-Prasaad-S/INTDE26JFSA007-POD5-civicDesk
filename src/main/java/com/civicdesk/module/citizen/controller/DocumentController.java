package com.civicdesk.module.citizen.controller;

import com.civicdesk.common.exception.citizen.InvalidRequestException;
import com.civicdesk.common.response.ApiResponse;
import com.civicdesk.module.citizen.dto.request.VerifyDocumentRequest;
import com.civicdesk.module.citizen.service.DocumentService;
import com.civicdesk.module.citizen.support.FileStorageService;
import com.civicdesk.module.citizen.support.IdGenerator;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

/**
 * Citizen document endpoints under base path {@code /citizenProfile} (served below the
 * application context path {@code /civicDesk}). {@code citizenId} on these operations is the
 * citizen's userId.
 *
 * <p>{@code uploadDocument} accepts a real {@code multipart/form-data} file: the bytes are written to
 * disk by {@link FileStorageService} under a generated name, and are streamed back by the
 * authorized {@link #downloadDocumentFile} endpoint. {@code status} values on the API are
 * single-character codes (V/E/R). JSON responses use the shared {@link ApiResponse} envelope; the
 * file download streams raw bytes and so is exempt.
 */
@RestController
@RequestMapping("/citizenProfile")
public class DocumentController {

    private final DocumentService documentService;
    private final FileStorageService fileStorage;

    public DocumentController(DocumentService documentService, FileStorageService fileStorage) {
        this.documentService = documentService;
        this.fileStorage = fileStorage;
    }

    /**
     * POST /{citizenId}/uploadDocument (multipart/form-data). Form parts: {@code file} (the
     * upload) and {@code documentType}. The file is stored on disk under a generated name; size/
     * type/limit rules are enforced by the service (and the stored file is rolled back if the
     * service rejects it).
     */
    @PostMapping(value = "/{citizenId}/uploadDocument", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CIT')")
    public ResponseEntity<ApiResponse> uploadDocument(
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

        try {
            String documentId = documentService.uploadDocument(
                    citizenId,
                    documentType,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    storedName);
            return ResponseEntity.status(201)
                    .body(ApiResponse.of("Document uploaded successfully", documentId));
        } catch (RuntimeException ex) {
            fileStorage.deleteQuietly(storedName); // roll back the stored file on validation/persist failure
            throw ex;
        }
    }

    /** GET /{citizenId}/getAllDocuments. 404 if the citizen does not exist. */
    @GetMapping("/{citizenId}/getAllDocuments")
    @PreAuthorize("hasRole('CIT')")
    public ResponseEntity<ApiResponse> getAllDocuments(@PathVariable String citizenId) {
        return ResponseEntity.ok(ApiResponse.data(documentService.getAllDocuments(citizenId)));
    }

    /** GET /{citizenId}/getDocumentById/{documentId}. Scoped to the owning citizen. */
    @GetMapping("/{citizenId}/getDocumentById/{documentId}")
    @PreAuthorize("hasRole('CIT')")
    public ResponseEntity<ApiResponse> getDocumentById(
            @PathVariable String citizenId,
            @PathVariable String documentId) {
        return ResponseEntity.ok(ApiResponse.data(documentService.getDocumentById(citizenId, documentId)));
    }

    /**
     * PUT /{citizenId}/verifyDocument/{documentId}. An officer ({@code FO}/{@code DS}/{@code ADM})
     * applies the manual status transition; the verifier's identity is taken from the JWT.
     */
    @PutMapping("/{citizenId}/verifyDocument/{documentId}")
    @PreAuthorize("hasAnyRole('FO','DS','ADM')")
    public ResponseEntity<ApiResponse> verifyDocument(
            @PathVariable String citizenId,
            @PathVariable String documentId,
            @Valid @RequestBody VerifyDocumentRequest request) {
        documentService.verifyDocument(citizenId, documentId, request);
        return ResponseEntity.ok(ApiResponse.of("Document verified successfully", null));
    }

    /**
     * GET /{citizenId}/documents/{documentId}/file — streams the document's bytes. Authorized to
     * the owning citizen or an officer (enforced in the service); replaces the old guessable
     * {@code /files/{filename}} route.
     */
    @GetMapping("/{citizenId}/documents/{documentId}/file")
    @PreAuthorize("hasAnyRole('CIT','FO','DS','ADM')")
    public ResponseEntity<Resource> downloadDocumentFile(
            @PathVariable String citizenId,
            @PathVariable String documentId) {
        String filename = documentService.resolveDownloadFileName(citizenId, documentId);
        Resource resource = fileStorage.load(filename);
        return ResponseEntity.ok()
                .contentType(contentTypeFor(extensionOf(filename)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    // ------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------

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
