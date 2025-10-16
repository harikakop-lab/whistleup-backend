package com.whistleup.backend.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message, null);
    }

    public ForbiddenException(String message, String details) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message, details);
    }
}
