package com.whistleup.backend.controllers;

import com.whistleup.backend.repository.DeviceTokenRepository;
import com.whistleup.backend.service.ExpoPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/whistleup/debug")
public class NotificationDebugController {

    private final DeviceTokenRepository deviceTokenRepository;
    private final ExpoPushService expoPushService;

    @PostMapping("/notify-me")
    public void notifyMe() {

        String token = deviceTokenRepository
            .findActiveTokensByUserId(9666499643L)
            .getFirst();

        expoPushService.send(
            token,
            "Maintenance",
            "Please pay your maintenance"
        );
    }
}