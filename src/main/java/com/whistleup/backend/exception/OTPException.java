package com.whistleup.backend.exception;

public class OTPException extends Exception {
    
    private final ErrorCode errorCode;
    
    public enum ErrorCode {
        OTP_NOT_FOUND,
        OTP_EXPIRED,
        INVALID_OTP,
        MAX_ATTEMPTS_EXCEEDED,
        OTP_ALREADY_VERIFIED,
        RESEND_COOLDOWN_ACTIVE,
        SMS_SEND_FAILED
    }
    
    public OTPException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}