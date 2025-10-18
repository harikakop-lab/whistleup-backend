package com.whistleup.backend.service.impl;

import com.whistleup.backend.entity.OTPEntity;
import com.whistleup.backend.exception.OTPException;
import com.whistleup.backend.repository.OTPRepository;
import com.whistleup.backend.resource.OTPResponse;
import com.whistleup.backend.resource.VerificationResponse;
import com.whistleup.backend.service.SMSProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.logging.Logger;

@Service
@Transactional
public class OTPService {

    private static final Logger logger = Logger.getLogger(OTPService.class.getName());

    @Autowired
    private OTPRepository otpRepository;

    @Autowired
    private SMSServiceFactory smsServiceFactory;

    @Value("${otp.length:4}")
    private int otpLength;

    @Value("${otp.validity.minutes:5}")
    private int otpValidityMinutes;

    @Value("${otp.max.attempts:3}")
    private int maxAttempts;

    @Value("${otp.resend.cooldown.seconds:60}")
    private int resendCooldownSeconds;

    private static final Random random = new Random();

    public OTPResponse generateOTP(String phoneNumber) throws Exception {
        // Validate phone number
        if (!isValidPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        // Get active SMS provider
        SMSProvider smsProvider = smsServiceFactory.getProvider();

        // Create OTP
        String otp = generateRandomOTP();
        OTPEntity otpEntity = new OTPEntity();
        otpEntity.setPhoneNumber(phoneNumber);
        otpEntity.setOtp(otp);
        otpEntity.setCreatedAt(LocalDateTime.now());
        otpEntity.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otpEntity.setAttemptCount(0);
        otpEntity.setLastResendTime(LocalDateTime.now());

        // Send via selected provider
        try {
            smsProvider.sendOTP(phoneNumber, otp, null);
            otpRepository.save(otpEntity);
            logger.info("OTP generated and sent via active provider");

            return new OTPResponse(
                    "OTP sent successfully",
                    5,
                    maskPhoneNumber(phoneNumber)
            );
        } catch (Exception e) {
            logger.severe("Failed to send OTP: " + e.getMessage());
            throw new Exception("Failed to send OTP: " + e.getMessage());
        }
    }

    public VerificationResponse verifyOTP(String phoneNumber, String otp) throws Exception {
        OTPEntity otpEntity = otpRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new OTPException(
                        "No OTP found for this phone number",
                        OTPException.ErrorCode.OTP_NOT_FOUND
                ));

        // Check if already verified
        if (otpEntity.getIsVerified()) {
            throw new OTPException(
                    "OTP already verified",
                    OTPException.ErrorCode.OTP_ALREADY_VERIFIED
            );
        }

        // Check if expired
        if (LocalDateTime.now().isAfter(otpEntity.getExpiresAt())) {
            throw new OTPException(
                    "OTP has expired",
                    OTPException.ErrorCode.OTP_EXPIRED
            );
        }

        // Check attempts
        if (otpEntity.getAttemptCount() >= maxAttempts) {
            throw new OTPException(
                    "Maximum attempts exceeded. Please request a new OTP",
                    OTPException.ErrorCode.MAX_ATTEMPTS_EXCEEDED
            );
        }

        // Verify OTP
        if (!otpEntity.getOtp().equals(otp)) {
            otpEntity.setAttemptCount(otpEntity.getAttemptCount() + 1);
            otpRepository.save(otpEntity);

            int remainingAttempts = maxAttempts - otpEntity.getAttemptCount();
            throw new OTPException(
                    "Invalid OTP. Attempts remaining: " + remainingAttempts,
                    OTPException.ErrorCode.INVALID_OTP
            );
        }

        // OTP verified
        otpEntity.setIsVerified(true);
        otpRepository.save(otpEntity);
        logger.info("OTP verified successfully for phone: " + maskPhoneNumber(phoneNumber));

        return new VerificationResponse(
                true,
                "OTP verified successfully",
                phoneNumber
        );
    }

    private String generateRandomOTP() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.matches("\\d{10}");
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}