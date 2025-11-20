package com.whistleup.backend.resource;

public class OTPResponse {
    private String message;
    private Integer validityMinutes;
    private Integer otp;
    private String maskedPhoneNumber;
    
    public OTPResponse(String message, Integer validityMinutes, String maskedPhoneNumber, Integer otp) {
        this.message = message;
        this.validityMinutes = validityMinutes;
        this.maskedPhoneNumber = maskedPhoneNumber;
        this.otp = otp;
    }
    
    public String getMessage() { return message; }
    public Integer getValidityMinutes() { return validityMinutes; }
    public Integer getOtp() { return otp; }
    public String getMaskedPhoneNumber() { return maskedPhoneNumber; }


}