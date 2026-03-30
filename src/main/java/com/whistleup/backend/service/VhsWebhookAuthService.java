package com.whistleup.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Validates inbound VHS → Nestiti webhook calls when {@code vhs.webhook-secret} is set.
 * Send the same value in header {@code X-VHS-Webhook-Secret}, or {@code Authorization: Bearer <secret>}.
 */
@Slf4j
@Service
public class VhsWebhookAuthService {

    @Value("${vhs.webhook-secret:}")
    private String configuredSecret;

    public boolean authorize(String secretHeader, String authorizationHeader) {
        if (!StringUtils.hasText(configuredSecret)) {
            log.warn("[vhs][webhook] vhs.webhook-secret is not set — webhook accepts unauthenticated calls");
            return true;
        }
        if (StringUtils.hasText(secretHeader) && configuredSecret.equals(secretHeader.trim())) {
            return true;
        }
        if (StringUtils.hasText(authorizationHeader)) {
            String auth = authorizationHeader.trim();
            if (auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
                String token = auth.substring(7).trim();
                if (configuredSecret.equals(token)) {
                    return true;
                }
            }
        }
        return false;
    }
}
