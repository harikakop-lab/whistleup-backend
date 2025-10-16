package com.whistleup.backend.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message, null);
    }

    public NotFoundException(String message, String details) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message, details);
    }
}
