package com.whistleup.backend.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.file-storage.root}")
    private String rootDir;

    private Path resolvedRootDir;

    @PostConstruct
    public void init() {
        resolvedRootDir = resolveWritableRootDir();
    }

    private Path resolveWritableRootDir() {
        Path configuredPath = null;
        if (rootDir != null && !rootDir.trim().isEmpty()) {
            configuredPath = Paths.get(rootDir.trim()).toAbsolutePath().normalize();
        }
        if (configuredPath != null) {
            try {
                Files.createDirectories(configuredPath);
                return configuredPath;
            } catch (IOException e) {
                log.warn("Configured file-storage path is not writable: {}. Falling back to local uploads directory.", configuredPath);
                log.debug("File-storage path init failure details", e);
            }
        }

        Path fallbackPath = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(fallbackPath);
            return fallbackPath;
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize writable upload directory", e);
        }
    }

    private Path getResolvedRootDir() {
        if (resolvedRootDir == null) {
            resolvedRootDir = resolveWritableRootDir();
        }
        return resolvedRootDir;
    }

    private Path getComplaintDir(Long complaintId) {
        return getResolvedRootDir().resolve("complaints").resolve(complaintId.toString());
    }

    private Path getMaintenancePaymentDir(Long maintenanceId) {
        return getResolvedRootDir().resolve("maintenance-payments").resolve(maintenanceId.toString());
    }

    public String saveComplaintFile(Long complaintId, MultipartFile file) {
        try {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path dir = getComplaintDir(complaintId);
            Files.createDirectories(dir);

            Path target = dir.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            return fileName; // IMPORTANT: return only file name
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public Resource loadComplaintFile(Long complaintId, String fileName) {
        try {
            Path filePath = getComplaintDir(complaintId).resolve(fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new RuntimeException("File not found");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid file path", e);
        }
    }

    public String saveMaintenancePaymentProof(Long maintenanceId, MultipartFile file) {
        try {
            String originalName = Objects.requireNonNullElse(file.getOriginalFilename(), "proof.jpg")
                    .replaceAll("[^a-zA-Z0-9._-]", "_");
            String fileName = System.currentTimeMillis() + "_" + originalName;
            Path dir = getMaintenancePaymentDir(maintenanceId);
            Files.createDirectories(dir);

            Path target = dir.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to store maintenance payment proof in " + getMaintenancePaymentDir(maintenanceId),
                    e
            );
        }
    }

    public Resource loadMaintenancePaymentProof(Long maintenanceId, String fileName) {
        try {
            Path filePath = getMaintenancePaymentDir(maintenanceId).resolve(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new RuntimeException("File not found");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid file path", e);
        }
    }
}
