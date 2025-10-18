package com.whistleup.backend.resource;

public class ErrorResponse {
    private String message;
    private String errorCode;
    
    public ErrorResponse(String message) {
        this.message = message;
    }
    
    public ErrorResponse(String message, String errorCode) {
        this.message = message;
        this.errorCode = errorCode;
    }
    
    public String getMessage() { return message; }
    public String getErrorCode() { return errorCode; }
}