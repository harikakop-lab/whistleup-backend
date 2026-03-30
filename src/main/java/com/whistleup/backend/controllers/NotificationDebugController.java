package com.whistleup.backend.controllers;

import com.whistleup.backend.entity.DeviceTokenEntity;
import com.whistleup.backend.repository.DeviceTokenRepository;
import com.whistleup.backend.service.ExpoPushService;
import com.whistleup.backend.service.FcmPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/whistleup/debug")
public class NotificationDebugController {

    private final DeviceTokenRepository deviceTokenRepository;
    private final ExpoPushService expoPushService;
    private final FcmPushService fcmPushService;

    @PostMapping("/notify-me")
    public void notifyMe() {
        List<DeviceTokenEntity> devices = deviceTokenRepository.findAll();
        if (devices.isEmpty()) {
            log.warn(
                    "[push][debug] no active device_tokens for userId={}. Log in on the phone so /devices/register runs.");
            return;
        }
        devices.forEach(d -> {
            boolean hasFcm = StringUtils.hasText(d.getFcmToken());
            boolean hasExpo = StringUtils.hasText(d.getExpoPushToken());
            boolean fcmReady = fcmPushService.isAvailable();
            log.info(
                    "[push][debug] row id={} hasFcm={} hasExpo={} fcmBeanReady={}",
                    d.getId(),
                    hasFcm,
                    hasExpo,
                    fcmReady);

            if (hasFcm && fcmReady) {
                log.info("[push][debug] using FCM path");
                fcmPushService.send(
                        d.getFcmToken(),
                        "Sample Notification",
                        "This is a sample notification for testing");
            } else if (hasExpo) {
                if (hasFcm && !fcmReady) {
                    log.warn("[push][debug] FCM token present but server FCM disabled; falling back to Expo");
                }
                log.info("[push][debug] using Expo path");
                expoPushService.send(
                        d.getExpoPushToken(),
                        "Sample Notification",
                        "This is a sample notification for testing");
            } else {
                log.warn("[push][debug] no fcm_token and no expo_push_token on row id={}", d.getId());
            }
        });
    }
}
