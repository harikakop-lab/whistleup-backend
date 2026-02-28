package com.whistleup.backend.exception;

public class ServiceOrderNotFoundException extends RuntimeException {
    public ServiceOrderNotFoundException(String message) {
        super(message);
    }
}