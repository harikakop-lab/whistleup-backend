package com.whistleup.backend.service;

import com.whistleup.backend.entity.Profile;
import com.whistleup.backend.entity.Users;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthenticationService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public String authenticate(Profile profile) {
        String username = profile.getEmail() != null ? profile.getEmail() : profile.getPhone();
        
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(username, profile.getPassword()));
        
        if (authentication.isAuthenticated()) {
            return jwtService.generateToken(username);
        }
        
        return "login failed";
    }
}