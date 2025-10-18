package com.whistleup.backend.service.impl;

import com.whistleup.backend.service.SMSProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SMSServiceFactory {
    
    @Value("${sms.provider:fast2sms}")
    private String activeProvider;
    
    @Autowired(required = false)
    private Fast2SMSProvider fast2smsProvider;

    /**
     * Get the active SMS provider based on configuration
     */
    public SMSProvider getProvider() throws Exception {
        switch (activeProvider.toLowerCase()) {
            case "fast2sms":
                if (fast2smsProvider == null) {
                    throw new Exception("Fast2SMS provider not configured");
                }
                return fast2smsProvider;
            default:
                throw new Exception("Unknown SMS provider: " + activeProvider);
        }
    }
}