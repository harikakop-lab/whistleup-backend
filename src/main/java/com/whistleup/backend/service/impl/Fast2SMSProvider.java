package com.whistleup.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whistleup.backend.service.SMSProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.logging.Logger;

@Service
public class Fast2SMSProvider implements SMSProvider {

    private static final Logger logger = Logger.getLogger(Fast2SMSProvider.class.getName());

    @Value("${sms.fast2sms.api.url:https://www.fast2sms.com/dev/bulkV2}")
    private String apiUrl;

    @Value("${sms.fast2sms.api.key}")
    private String apiKey;

    @Value("${sms.fast2sms.route:otp}")
    private String route;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public Fast2SMSProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean sendOTP(String phoneNumber, String otp, String message) throws Exception {
        try {
            // Validate phone number
            if (!isValidPhoneNumber(phoneNumber)) {
                throw new IllegalArgumentException("Invalid phone number format");
            }

            // Build the request
            String fullMessage = message != null ? message : "Your OTP is: " + otp;

            String url = buildUrl(phoneNumber, otp, fullMessage);

            // Make the API call
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                // Parse response
                JsonNode responseBody = objectMapper.readTree(response.getBody());
                boolean success = responseBody.get("success").asBoolean();

                if (success) {
                    logger.info("OTP sent successfully via Fast2SMS to: " + maskPhoneNumber(phoneNumber));
                    return true;
                } else {
                    String error = responseBody.get("message").asText();
                    throw new Exception("Fast2SMS Error: " + error);
                }
            } else {
                throw new Exception("Fast2SMS API returned status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.severe("Failed to send SMS via Fast2SMS: " + e.getMessage());
            throw new Exception("Failed to send OTP via Fast2SMS: " + e.getMessage());
        }
    }

    private String buildUrl(String phoneNumber, String otp, String message) {
        return apiUrl + "?" +
                "authorization=" + apiKey +
                "&variables_values=" + otp +
                "&route=" + route +
                "&numbers=" + phoneNumber +
                "&message=" + encodeMessage(message);
    }

    private String encodeMessage(String message) {
        try {
            return java.net.URLEncoder.encode(message, "UTF-8");
        } catch (Exception e) {
            return message;
        }
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
