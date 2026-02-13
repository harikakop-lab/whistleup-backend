package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.ContactResource;
import com.whistleup.backend.resource.ProfileCreateResource;
import com.whistleup.backend.resource.ProfileResponseResource;
import com.whistleup.backend.service.ProfileService;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/whistleup/profile")
@CrossOrigin(origins = "*")
public class ProfileController {

    private final ProfileService profileService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    private Path uploadPath;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PostConstruct
    public void init() throws IOException {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);
    }

    @GetMapping("/{username}")
    public ResponseEntity<ProfileResponseResource> getProfileByUsername(@PathVariable("username") String username) {
        ProfileResponseResource profileResponseResource = profileService.getProfileByUsername(username);
        profileResponseResource.setPassword(null);
        profileResponseResource.setAvatarUri("http://16.170.115.179:8080/whistleup/profile/" + username + "/image");
        return new ResponseEntity<>(profileResponseResource, HttpStatus.OK);
    }

    @PostMapping("/create")
    public ResponseEntity<ProfileResponseResource> createProfile(@RequestBody ProfileCreateResource profileCreateResource) {
        var profileResponse = profileService.createProfile(profileCreateResource);
        profileResponse.setPassword(null);
        return new ResponseEntity<>(profileResponse, HttpStatus.CREATED);
    }

    @PatchMapping("/update")
    public ResponseEntity<String> updateProfile(@RequestBody ProfileCreateResource profileUpdateResource) {
        String response = profileService.updateProfile(profileUpdateResource);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(value = "/{username}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadProfileImage(@PathVariable String username, @RequestParam("file") MultipartFile file) throws IOException {
        profileService.uploadProfileImage(username, file);
        return new ResponseEntity<>("Profile image uploaded successfully", HttpStatus.OK);
    }

    @GetMapping("/image/{username}")
    public ResponseEntity<Resource> getProfileImage(@PathVariable String username) throws IOException {
        Resource resource = profileService.getProfileImage(username);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(resource);
    }

    @GetMapping("/{username}/contacts")
    public ResponseEntity<List<ContactResource>> getContactsByUsername(@PathVariable("username") String username) {
        List<ContactResource> response = profileService.getContactsByUsername(username);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<String> deleteProfile(@PathVariable("userId") String userId) {
        String response = profileService.deleteProfile(userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /* -------------------------------------------------
       1️⃣ Upload Profile Image
    ------------------------------------------------- */
    @PostMapping("/{username}/upload")
    public ResponseEntity<?> uploadProfilePicture(@PathVariable String username, @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }
            // Validate file type
            String contentType = file.getContentType();
            if (Objects.isNull(contentType) ||
                    !(contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/jpg"))) {
                return ResponseEntity.badRequest().body("Only JPG/PNG allowed");
            }
            // Force filename = username.jpg
            String fileName = username + ".jpg";
            Path targetLocation = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return ResponseEntity.ok("Upload successful");
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().body("Could not store file");
        }
    }

    /* -------------------------------------------------
       2️⃣ Get Profile Image
    ------------------------------------------------- */
    @GetMapping("/{username}/image")
    public ResponseEntity<Resource> getProfilePicture(@PathVariable String username) {
        try {
            Path filePath = uploadPath.resolve(username + ".jpg").normalize();
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .body(resource);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
