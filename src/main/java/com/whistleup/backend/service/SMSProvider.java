package com.whistleup.backend.service;

public interface SMSProvider {
    /**
     * Send OTP via SMS
     * @param phoneNumber - 10 digit phone number
     * @param otp - OTP code
     * @param message - Custom message (optional)
     * @return true if sent successfully, false otherwise
     * @throws Exception if sending fails
     */
    boolean sendOTP(String phoneNumber, String otp, String message) throws Exception;
}