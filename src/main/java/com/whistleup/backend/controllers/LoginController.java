package com.whistleup.backend.controllers;

import com.whistleup.backend.constants.Roles;
import com.whistleup.backend.exception.BadRequestException;
import com.whistleup.backend.resource.LoginRequest;
import com.whistleup.backend.resource.LoginResponse;
import com.whistleup.backend.resource.ProfileResponseResource;
import com.whistleup.backend.service.AuthenticationService;
import com.whistleup.backend.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/whistleup/users")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LoginController {

    private final AuthenticationService authenticationService;
    private final ProfileService profileService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> logIn(@Valid @RequestBody LoginRequest loginRequest) {
        final String username = resolveUsername(loginRequest);
        final String secret = resolveSecret(loginRequest);
        final ProfileResponseResource profile = profileService.getProfileByUsername(username);
        if ((profile.getRole() == Roles.USER || profile.getRole() == Roles.OWNER)
                && !Boolean.TRUE.equals(profile.getIsAssigned())) {
            throw new BadRequestException(
                    "Your profile is waiting for admin approval.",
                    "Please wait until your apartment admin assigns your flat."
            );
        }
        final String token = authenticationService.authenticate(username, secret);

        LoginResponse loginResponse = LoginResponse.builder()
                .jwtToken(token)
                .username(username)
                .profileId(username)
                .role(profile.getRole())
                .build();

        return new ResponseEntity<>(loginResponse, HttpStatus.OK);
    }

    private String resolveUsername(LoginRequest loginRequest) {
        if (hasText(loginRequest.getPhone())) {
            return loginRequest.getPhone().trim();
        }
        if (hasText(loginRequest.getEmail())) {
            return loginRequest.getEmail().trim();
        }
        throw new IllegalArgumentException("Either phone or email is required");
    }

    private String resolveSecret(LoginRequest loginRequest) {
        if (hasText(loginRequest.getPin())) {
            return loginRequest.getPin().trim();
        }
        throw new IllegalArgumentException("Either pin or password is required");
    }

    private boolean hasText(String value) {
        return Objects.nonNull(value) && !value.trim().isEmpty();
    }
}