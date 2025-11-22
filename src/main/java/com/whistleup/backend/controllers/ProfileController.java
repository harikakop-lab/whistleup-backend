package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.ProfileCreateResource;
import com.whistleup.backend.resource.ProfileResponseResource;
import com.whistleup.backend.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<String> deleteProfile(@PathVariable("userId") String userId) {
        String response = profileService.deleteProfile(userId);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
