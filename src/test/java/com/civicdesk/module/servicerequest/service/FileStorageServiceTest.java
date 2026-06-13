package com.civicdesk.module.serviceRequest.service;

import com.civicdesk.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FileStorageService}: file-type validation and the on-disk layout.
 * Backed by a JUnit {@link TempDir} so real files are written and cleaned up automatically.
 */
class FileStorageServiceTest {

    @TempDir
    Path uploadsDir;

    private FileStorageService service;

    @BeforeEach
    void setUp() {
        service = new FileStorageService(uploadsDir.toString());
    }

    @Test
    @DisplayName("stores a valid PDF under the per-request folder and returns its relative path")
    void storesValidFile() {
        MultipartFile file = new MockMultipartFile("file", "id.pdf", "application/pdf", "data".getBytes());

        String relativePath = service.store(file, "REQ-1", "CIT-1");

        assertThat(relativePath).startsWith("REQ-1/");
        assertThat(relativePath).contains("CIT-1_REQ-1_");
        assertThat(relativePath).endsWith(".pdf");
        assertThat(Files.exists(uploadsDir.resolve(relativePath))).isTrue();
    }

    @Test
    @DisplayName("accepts case-insensitive allowed extensions")
    void acceptsUppercaseExtension() {
        MultipartFile file = new MockMultipartFile("file", "scan.PNG", "image/png", "data".getBytes());

        String relativePath = service.store(file, "REQ-1", "CIT-1");

        assertThat(relativePath).endsWith(".png");
    }

    @Test
    @DisplayName("rejects an empty file")
    void rejectsEmptyFile() {
        MultipartFile file = new MockMultipartFile("file", "id.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.store(file, "REQ-1", "CIT-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("rejects an unsupported file type")
    void rejectsUnsupportedType() {
        MultipartFile file = new MockMultipartFile("file", "malware.exe", "application/octet-stream",
                "data".getBytes());

        assertThatThrownBy(() -> service.store(file, "REQ-1", "CIT-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid file type");
    }

    @Test
    @DisplayName("rejects a file with no extension")
    void rejectsNoExtension() {
        MultipartFile file = new MockMultipartFile("file", "noextension", "application/pdf",
                "data".getBytes());

        assertThatThrownBy(() -> service.store(file, "REQ-1", "CIT-1"))
                .isInstanceOf(BadRequestException.class);
    }
}
