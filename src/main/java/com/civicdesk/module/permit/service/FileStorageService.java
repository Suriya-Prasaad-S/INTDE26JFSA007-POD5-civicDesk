package com.civicdesk.module.permit.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public String store(MultipartFile file, String subfolder) {
        try {
            Path dir = Paths.get(uploadDir, subfolder);
            Files.createDirectories(dir);
            String name = (file.getOriginalFilename() != null)
                    ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                    : "file";
            Files.copy(file.getInputStream(), dir.resolve(name),
                    StandardCopyOption.REPLACE_EXISTING);
            return "/" + uploadDir + "/" + subfolder + "/" + name;
        } catch (IOException e) {
            throw new RuntimeException("File storage failed: " + e.getMessage(), e);
        }
    }
}