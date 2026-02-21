package com.whistleup.backend.service;

import com.whistleup.backend.notifications.entity.NotificationEntity;
import com.whistleup.backend.repository.DeviceTokenRepository;
import com.whistleup.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationSendService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final ExpoPushService expoPushService;

    /**
     * Send notification to ONE user
     */
    @Transactional
    public void notifyUser(Long userId, String title, String body, String type) {

        // 1️⃣ Save to DB
        notificationRepository.save(
                NotificationEntity.builder()
                        .userId(userId)
                        .title(title)
                        .body(body)
                        .type(type)
                        .isRead(false)
                        .build()
        );

        // 2️⃣ Fetch active tokens
        List<String> tokens =
                deviceTokenRepository.findActiveTokensByUserId(userId);

        // 3️⃣ Send push
        tokens.forEach(token ->
                expoPushService.send(token, title, body)
        );
    }
}