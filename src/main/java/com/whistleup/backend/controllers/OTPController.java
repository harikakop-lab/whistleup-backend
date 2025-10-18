package com.whistleup.backend.controllers;

import com.whistleup.backend.service.impl.OTPService;
import com.whistleup.backend.exception.OTPException;
import com.whistleup.backend.resource.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/otp")
@CrossOrigin(origins = "*")
public class OTPController {
    
    @Autowired
    private OTPService otpService;
    
    /**
     * Request OTP
     * POST: /api/otp/send
     * Body: {"phoneNumber": "9876543210"}
     */
    @PostMapping("/send")
    public ResponseEntity<?> requestOTP(@RequestBody OTPRequest request) {
        try {
            if (request.getPhoneNumber() == null || request.getPhoneNumber().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Phone number is required"));
            }
            
            OTPResponse response = otpService.generateOTP(request.getPhoneNumber());
            return ResponseEntity.ok(response);
            
        } catch (OTPException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage(), e.getErrorCode().name()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("An unexpected error occurred"));
        }
    }
    
    /**
     * Verify OTP
     * POST: /api/otp/verify
     * Body: {"phoneNumber": "9876543210", "otp": "1234"}
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyOTP(@RequestBody VerifyOTPRequest request) {
        try {
            if (request.getPhoneNumber() == null || request.getOtp() == null) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Phone number and OTP are required"));
            }
            
            VerificationResponse response = otpService.verifyOTP(
                request.getPhoneNumber(),
                request.getOtp()
            );
            return ResponseEntity.ok(response);
            
        } catch (OTPException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage(), e.getErrorCode().name()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("An unexpected error occurred"));
        }
    }
}