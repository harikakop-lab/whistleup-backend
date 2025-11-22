package com.whistleup.backend.controllers;

import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.resource.LoginRequest;
import com.whistleup.backend.resource.LoginResponse;
import com.whistleup.backend.service.AuthenticationService;
import com.whistleup.backend.service.ProfileService;
import lombok.extern.java.Log;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whistleup/users")
@CrossOrigin(origins = "*")
public class LoginController {

    private final AuthenticationService authenticationService;
    private final ProfileService profileService;
    public LoginController(AuthenticationService authenticationService, ProfileService profileService) {
        this.authenticationService = authenticationService;
        this.profileService = profileService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> logIn(@RequestBody LoginRequest loginRequest) {
        Profile profile = Profile.builder().build();
        BeanUtils.copyProperties(loginRequest, profile);
        LoginResponse loginResponse = LoginResponse.builder().build();
        String username = profile.getEmail() != null ? profile.getEmail() : profile.getPhone();
        loginResponse.setJwtToken(authenticationService.authenticate(profile));
        loginResponse.setRole(profileService.getRole(username));
        return new ResponseEntity<>(loginResponse, HttpStatus.OK);
    }


}