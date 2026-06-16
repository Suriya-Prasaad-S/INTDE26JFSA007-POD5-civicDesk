package com.civicdesk.module.citizen.support;

import com.civicdesk.module.citizen.exception.InvalidRequestException;
import com.civicdesk.module.citizen.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Stores uploaded document bytes on the local disk and serves them back. The directory is
 * configurable via {@code citizen.document.storage-dir} (default {@code ./uploads}); files are
 * written under a generated name (never the user's original name) to prevent path traversal.
 */
@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${citizen.document.storage-dir:./uploads}") String dir) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory: " + root, e);
        }
    }

    /** Writes {@code content} under {@code storedName}; returns the stored name. */
    public String store(InputStream content, String storedName) {
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw new InvalidRequestException("Invalid stored file name: " + storedName);
        }
        try {
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file: " + storedName, e);
        }
        return storedName;
    }

    /** Loads a stored file as a {@link Resource} (404 if it is missing or unreadable). */
    public Resource load(String filename) {
        Path file = root.resolve(filename).normalize();
        if (!file.startsWith(root)) {
            throw new InvalidRequestException("Invalid file name: " + filename);
        }
        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("File not found: " + filename);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File not found: " + filename);
        }
    }

    /** Best-effort delete — used to roll back a stored file when the service rejects the upload. */
    public void deleteQuietly(String storedName) {
        try {
            Files.deleteIfExists(root.resolve(storedName).normalize());
        } catch (IOException ignored) {
            // best-effort cleanup; nothing to do
        }
    }
}
