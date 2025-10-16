package com.whistleup.backend.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message, null);
    }

    public UnauthorizedException(String message, String details) {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message, details);
    }
}
