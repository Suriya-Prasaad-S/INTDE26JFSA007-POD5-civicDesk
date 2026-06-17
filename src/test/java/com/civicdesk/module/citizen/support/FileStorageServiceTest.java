package com.civicdesk.module.citizen.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import com.civicdesk.common.exception.citizen.InvalidRequestException;
import com.civicdesk.common.exception.citizen.ResourceNotFoundException;

/** Unit tests for {@link FileStorageService} against a real temporary directory. */
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService storage;

    @BeforeEach
    void setup() {
        storage = new FileStorageService(tempDir.toString());
    }

    private static InputStream bytes(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void store_writesFileAndReturnsName() throws Exception {
        String name = storage.store(bytes("hello"), "doc-1.pdf");

        assertThat(name).isEqualTo("doc-1.pdf");
        Path written = tempDir.resolve("doc-1.pdf");
        assertThat(Files.exists(written)).isTrue();
        assertThat(Files.readString(written)).isEqualTo("hello");
    }

    @Test
    void store_overwritesExistingFile() throws Exception {
        storage.store(bytes("first"), "doc-1.pdf");
        storage.store(bytes("second"), "doc-1.pdf");

        assertThat(Files.readString(tempDir.resolve("doc-1.pdf"))).isEqualTo("second");
    }

    @Test
    void store_pathTraversalName_throwsInvalidRequest() {
        assertThatThrownBy(() -> storage.store(bytes("x"), "../escape.pdf"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void load_existingFile_returnsReadableResource() {
        storage.store(bytes("hello"), "doc-1.pdf");

        Resource resource = storage.load("doc-1.pdf");

        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
    }

    @Test
    void load_missingFile_throwsResourceNotFound() {
        assertThatThrownBy(() -> storage.load("nope.pdf"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void load_pathTraversalName_throwsInvalidRequest() {
        assertThatThrownBy(() -> storage.load("../../etc/passwd"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void deleteQuietly_removesStoredFile() {
        storage.store(bytes("hello"), "doc-1.pdf");
        assertThat(Files.exists(tempDir.resolve("doc-1.pdf"))).isTrue();

        storage.deleteQuietly("doc-1.pdf");

        assertThat(Files.exists(tempDir.resolve("doc-1.pdf"))).isFalse();
    }

    @Test
    void deleteQuietly_missingFile_isNoOp() {
        // Must not throw even when the file is absent (best-effort rollback).
        storage.deleteQuietly("never-existed.pdf");
    }
}
