package com.whistleup.backend.controllers;

import com.whistleup.backend.entity.DeviceTokenEntity;
import com.whistleup.backend.repository.DeviceTokenRepository;
import com.whistleup.backend.service.ExpoPushService;
import com.whistleup.backend.service.FcmPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/whistleup/debug")
public class NotificationDebugController {

    private final DeviceTokenRepository deviceTokenRepository;
    private final ExpoPushService expoPushService;
    private final FcmPushService fcmPushService;

    @PostMapping("/notify-me")
    public void notifyMe() {
        List<DeviceTokenEntity> devices =
                deviceTokenRepository.findByUserIdAndActiveTrue(9666499643L);
        if (devices.isEmpty()) {
            return;
        }
        DeviceTokenEntity d = devices.getFirst();
        if (StringUtils.hasText(d.getFcmToken()) && fcmPushService.isAvailable()) {
            fcmPushService.send(
                    d.getFcmToken(),
                    "Maintenance",
                    "Please pay your maintenance");
        } else if (StringUtils.hasText(d.getExpoPushToken())) {
            expoPushService.send(
                    d.getExpoPushToken(),
                    "Maintenance",
                    "Please pay your maintenance");
        }
    }
}
