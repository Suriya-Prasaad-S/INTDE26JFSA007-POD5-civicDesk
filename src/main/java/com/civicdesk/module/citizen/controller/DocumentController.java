package com.civicdesk.module.citizen.controller;

import com.civicdesk.common.response.ApiResponse;
import com.civicdesk.module.citizen.service.DocumentService;
import com.civicdesk.module.citizen.support.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Citizen document-wallet endpoints under base path {@code /citizenProfile}. These are the
 * citizen's read views of their wallet (government-issued documents pushed by the service-request
 * module via {@code DocumentService.addDocument}). All are CIT-only and scoped to the caller's own
 * documents. JSON responses use {@link ApiResponse}; the file download streams raw bytes.
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

    /** GET /{citizenId}/getAllDocuments — the caller's own wallet. */
    @GetMapping("/{citizenId}/getAllDocuments")
    @PreAuthorize("hasRole('CIT')")
    public ResponseEntity<ApiResponse> getAllDocuments(@PathVariable String citizenId) {
        return ResponseEntity.ok(ApiResponse.data(documentService.getAllDocuments(citizenId)));
    }

    /** GET /{citizenId}/getDocumentById/{documentId} — one of the caller's own documents. */
    @GetMapping("/{citizenId}/getDocumentById/{documentId}")
    @PreAuthorize("hasRole('CIT')")
    public ResponseEntity<ApiResponse> getDocumentById(
            @PathVariable String citizenId,
            @PathVariable String documentId) {
        return ResponseEntity.ok(ApiResponse.data(documentService.getDocumentById(citizenId, documentId)));
    }

    /** GET /{citizenId}/documents/{documentId}/file — streams the caller's own document bytes. */
    @GetMapping("/{citizenId}/documents/{documentId}/file")
    @PreAuthorize("hasRole('CIT')")
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

    // --- Helpers ---

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
