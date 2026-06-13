package com.civicdesk.module.serviceRequest.service;

import com.civicdesk.common.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Stores uploaded documents on the local filesystem (Phase 1 - cloud storage deferred).
 *
 * <p>Layout: every file is written inside a per-request folder under the configured uploads
 * root, and the file name carries the citizen id and request id, i.e.
 * {@code <uploads.dir>/<requestId>/<citizenId>_<requestId>_<uuid>.<ext>}. The uploads root is
 * created on startup. Only PDF/JPG/PNG are accepted; the path relative to the uploads root
 * is returned and persisted on the document row.</p>
 */
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");

    private final Path rootDir;

    public FileStorageService(@Value("${civicdesk.uploads.dir:./uploads}") String uploadsDir) {
        this.rootDir = Paths.get(uploadsDir).toAbsolutePath().normalize();
        try {
            // Make sure the uploads folder exists up front, before any document is uploaded.
            Files.createDirectories(rootDir);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not create the uploads directory: " + rootDir, ex);
        }
    }

    /**
     * Validates the file type and stores it inside the request's folder, naming the file with
     * the citizen id and request id.
     *
     * @param requestId the request the document belongs to (also the per-request folder name)
     * @param citizenId the citizen who owns the request
     * @return the stored file path relative to the uploads root,
     *         e.g. {@code <requestId>/<citizenId>_<requestId>_<uuid>.pdf}
     * @throws BadRequestException if the file is empty or of an unsupported type
     */
    public String store(MultipartFile file, String requestId, String citizenId) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file is missing or empty");
        }

        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(original);
        extension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Invalid file type. Only PDF, JPG, and PNG files are accepted");
        }

        try {
            // Each request gets its own folder, created on first upload.
            Path targetDir = rootDir.resolve(requestId).normalize();
            Files.createDirectories(targetDir);

            // Name carries the citizen id and request id; a UUID keeps multiple uploads unique.
            String storedName = safe(citizenId) + "_" + safe(requestId) + "_"
                    + UUID.randomUUID() + "." + extension;
            Path target = targetDir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            // Return a path relative to the uploads root, using forward slashes.
            return rootDir.relativize(target).toString().replace('\\', '/');
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to store uploaded file", ex);
        }
    }

    /** Filesystem-safe form of an id for use in a file name. */
    private String safe(String value) {
        return value == null ? "" : value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
