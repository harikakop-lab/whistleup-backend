package com.whistleup.backend.resource;

public class OTPResponse {
    private String message;
    private Integer validityMinutes;
    private String maskedPhoneNumber;
    
    public OTPResponse(String message, Integer validityMinutes, String maskedPhoneNumber) {
        this.message = message;
        this.validityMinutes = validityMinutes;
        this.maskedPhoneNumber = maskedPhoneNumber;
    }
    
    public String getMessage() { return message; }
    public Integer getValidityMinutes() { return validityMinutes; }
    public String getMaskedPhoneNumber() { return maskedPhoneNumber; }
}