package com.whistleup.backend.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONFLICT", message, null);
    }

    public ConflictException(String message, String details) {
        super(HttpStatus.CONFLICT, "CONFLICT", message, details);
    }
}
