package com.whistleup.backend.resource;

public class VerificationResponse {
    private Boolean verified;
    private String message;
    private String phoneNumber;
    
    public VerificationResponse(Boolean verified, String message, String phoneNumber) {
        this.verified = verified;
        this.message = message;
        this.phoneNumber = phoneNumber;
    }
    
    public Boolean getVerified() { return verified; }
    public String getMessage() { return message; }
    public String getPhoneNumber() { return phoneNumber; }
}