package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.ContactResource;
import com.whistleup.backend.resource.ProfileCreateResource;
import com.whistleup.backend.resource.ProfileResponseResource;
import com.whistleup.backend.service.ProfileService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/whistleup/profile")
@CrossOrigin(origins = "*")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/{username}")
    public ResponseEntity<ProfileResponseResource> getProfileByUsername(@PathVariable("username") String username) {
        ProfileResponseResource profileResponseResource = profileService.getProfileByUsername(username);
        return new ResponseEntity<>(profileResponseResource, HttpStatus.OK);
    }

    @PostMapping("/create")
    public ResponseEntity<ProfileResponseResource> createProfile(@RequestBody ProfileCreateResource profileCreateResource) {
        var profileResponse = profileService.createProfile(profileCreateResource);
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
}
