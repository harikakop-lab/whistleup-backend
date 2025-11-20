package com.whistleup.backend.service.impl;

import com.whistleup.backend.service.SMSProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;

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
        if ("fast2sms".equalsIgnoreCase(activeProvider)) {
            if (Objects.isNull(fast2smsProvider)) {
                throw new Exception("Fast2SMS provider not configured");
            }
            return fast2smsProvider;
        }
        throw new Exception("Unknown SMS provider: " + activeProvider);
    }
}