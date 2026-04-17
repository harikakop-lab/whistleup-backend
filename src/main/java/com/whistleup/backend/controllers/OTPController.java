package com.whistleup.backend.controllers;

import com.whistleup.backend.service.ProfileService;
import com.whistleup.backend.service.impl.OTPService;
import com.whistleup.backend.exception.OTPException;
import com.whistleup.backend.resource.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("whistleup/api/otp")
@CrossOrigin(origins = "*")
public class OTPController {
    
    @Autowired
    private OTPService otpService;

    @Autowired
    private ProfileService profileService;
    
    /**
     * Request OTP
     * POST: /api/otp/send
     * Body: {"phoneNumber": "9876543210"}
     */
    @PostMapping("/send")
    public ResponseEntity<?> requestOTP(@RequestBody OTPRequest request, @RequestParam(value = "forgotPassword", required = false) boolean forgotPasswordFlow) {
        try {
            if (request.getPhoneNumber() == null || request.getPhoneNumber().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Phone number is required"));
            }

            if (forgotPasswordFlow) {
                if (!profileService.doesProfileExists(request.getPhoneNumber())) {
                    return ResponseEntity.badRequest()
                            .body(new ErrorResponse("Phone number does not exists"));
                }
            }
            
            OTPResponse response = new OTPResponse("OTP sent successfully", 5, "****1234", 1234);
            // OTPResponse otpResponse = otpService.generateOTP(request.getPhoneNumber());
//            response.setOtp(1234);
            // response.setMaskedPhoneNumber("****1234");
            return ResponseEntity.ok(response);
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