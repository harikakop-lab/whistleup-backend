package com.whistleup.backend.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, null);
    }

    public BadRequestException(String message, String details) {
        super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, details);
    }
}
