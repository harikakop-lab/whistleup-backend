package com.whistleup.backend.controllers;

import com.whistleup.backend.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/whistleup/issues")
@CrossOrigin("*")
public class MediaController {

    private final FileStorageService storageService;

    public MediaController(FileStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/{complaintId}/{fileName}")
    public ResponseEntity<Resource> getComplaintImage(
            @PathVariable Long complaintId,
            @PathVariable String fileName,
            HttpServletRequest request) {

        Resource resource = storageService.loadComplaintFile(complaintId, fileName);

        String contentType;
        try {
            contentType = request.getServletContext()
                    .getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .body(resource);
    }
}
