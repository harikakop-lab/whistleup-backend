package com.whistleup.backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.whistleup.backend.service.ProfileService;

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
        try {
            profileService.createProfile(profileCreateResource);
            return new ResponseEntity<>(new ProfileResponseResource(), HttpStatus.CREATED);
        } catch (Exception exception) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
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
