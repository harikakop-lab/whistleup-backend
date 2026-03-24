package com.whistleup.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final ExpoPushService expoPushService;

    public void sendPush(
            String pushToken,
            String title,
            String body,
            Map<String, Object> data
    ) {
        expoPushService.sendWithData(pushToken, title, body, data);
    }
}
