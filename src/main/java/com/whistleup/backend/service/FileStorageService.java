package com.whistleup.backend.service;

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

@Service
public class FileStorageService {

    @Value("${app.file-storage.root}")
    private String rootDir;

    private Path getComplaintDir(Long complaintId) {
        return Paths.get(rootDir, "complaints", complaintId.toString());
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
}
