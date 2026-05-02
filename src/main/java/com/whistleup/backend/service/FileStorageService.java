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

    private Path getPackersMoversDir(Long inquiryId) {
        return getResolvedRootDir().resolve("packers-movers").resolve(inquiryId.toString());
    }

    private Path getMaintenanceLedgerDir(String buildingId, int year, int month) {
        String safeBuilding = sanitizePathSegment(buildingId);
        return getResolvedRootDir()
                .resolve("maintenance-ledger")
                .resolve(safeBuilding)
                .resolve(String.valueOf(year))
                .resolve(String.valueOf(month));
    }

    private String sanitizePathSegment(String raw) {
        if (raw == null || raw.isBlank()) {
            return "unknown";
        }
        return raw.replaceAll("[^0-9A-Za-z._-]", "_");
    }

    private String sanitizeProfilePhoneSegment(String phone) {
        if (phone == null || phone.isBlank()) {
            return "unknown";
        }
        return phone.replaceAll("[^0-9A-Za-z+._-]", "_");
    }

    private Path getProfileTenantDocDir(String phone) {
        return getResolvedRootDir().resolve("profile-tenant-docs").resolve(sanitizeProfilePhoneSegment(phone));
    }

    public String saveProfileTenantDocument(String phone, MultipartFile file) {
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new RuntimeException("File exceeds maximum size of 5 MB");
        }
        try {
            String originalName = Objects.requireNonNullElse(file.getOriginalFilename(), "document.bin")
                    .replaceAll("[^a-zA-Z0-9._-]", "_");
            String fileName = System.currentTimeMillis() + "_" + originalName;
            Path dir = getProfileTenantDocDir(phone);
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store tenant document for " + phone, e);
        }
    }

    public Resource loadProfileTenantDocument(String phone, String fileName) {
        try {
            if (fileName == null || fileName.isBlank() || fileName.contains("..")) {
                throw new RuntimeException("Invalid file name");
            }
            Path filePath = getProfileTenantDocDir(phone).resolve(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new RuntimeException("File not found");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid file path", e);
        }
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

    public String savePackersMoversWalkthrough(Long inquiryId, MultipartFile file) {
        try {
            String originalName = Objects.requireNonNullElse(file.getOriginalFilename(), "walkthrough.mp4")
                    .replaceAll("[^a-zA-Z0-9._-]", "_");
            String fileName = System.currentTimeMillis() + "_" + originalName;
            Path dir = getPackersMoversDir(inquiryId);
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store walkthrough video", e);
        }
    }

    public String saveMaintenanceLedgerAttachment(String buildingId, int year, int month, MultipartFile file) {
        try {
            String originalName = Objects.requireNonNullElse(file.getOriginalFilename(), "attachment.bin")
                    .replaceAll("[^a-zA-Z0-9._-]", "_");
            String fileName = System.currentTimeMillis() + "_" + originalName;
            Path dir = getMaintenanceLedgerDir(buildingId, year, month);
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store maintenance ledger attachment", e);
        }
    }

    public Resource loadMaintenanceLedgerAttachment(String buildingId, int year, int month, String fileName) {
        try {
            if (fileName == null || fileName.isBlank() || fileName.contains("..")) {
                throw new RuntimeException("Invalid file name");
            }
            Path filePath = getMaintenanceLedgerDir(buildingId, year, month).resolve(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new RuntimeException("File not found");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid file path", e);
        }
    }

    public void deleteMaintenanceLedgerStoredFiles(String buildingId, int year, int month, java.util.List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return;
        }
        Path dir = getMaintenanceLedgerDir(buildingId, year, month);
        for (String name : fileNames) {
            if (name == null || name.isBlank() || name.contains("..")) {
                continue;
            }
            try {
                Path p = dir.resolve(name).normalize();
                if (!p.startsWith(dir.normalize())) {
                    continue;
                }
                Files.deleteIfExists(p);
            } catch (IOException ignored) {
                log.warn("Could not delete maintenance ledger file {}", name);
            }
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
