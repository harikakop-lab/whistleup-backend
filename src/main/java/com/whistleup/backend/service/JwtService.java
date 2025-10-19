package com.whistleup.backend.service;

public interface JwtService {

    String generateToken(String userName);
}
