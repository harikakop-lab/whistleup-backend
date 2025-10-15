package com.whistleup.backend.controllers;

import com.whistleup.backend.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whistleup/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("")
    public ResponseEntity<String> sayHello() {
        return new ResponseEntity<>("Hello, Profile", HttpStatus.OK);
    }

    @PostMapping("/create")
    public ResponseEntity<ProfileResponseResource> createProfile(@RequestBody ProfileCreateResource profileCreateResource) {
        profileService.createProfile(profileCreateResource);
        return new ResponseEntity<>(new ProfileResponseResource(), HttpStatus.CREATED);
    }

    @PatchMapping("/update")
    public ResponseEntity<ProfileResponseResource> updateProfile(@RequestBody ProfileCreateResource profileUpdateResource) {
        profileService.updateProfile(profileUpdateResource);
        return new ResponseEntity<>(new ProfileResponseResource(), HttpStatus.OK);
    }

    @PutMapping("/password/change")
    public ResponseEntity<Void> changePassword() {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
