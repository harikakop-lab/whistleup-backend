package com.whistleup.backend.service;

import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {

    String generateToken(String userName);

    String extractToken(String token);

    boolean validateToken(String token, UserDetails userDetails);
}
