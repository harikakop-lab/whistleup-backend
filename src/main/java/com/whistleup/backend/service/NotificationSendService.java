package com.whistleup.backend.service;

import com.whistleup.backend.entity.DeviceTokenEntity;
import com.whistleup.backend.entity.NotificationEntity;
import com.whistleup.backend.repository.DeviceTokenRepository;
import com.whistleup.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationSendService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final ExpoPushService expoPushService;
    private final FcmPushService fcmPushService;

    /**
     * Send notification to ONE user
     */
    @Transactional
    public void notifyUser(Long userId, String title, String body, String type) {
        LocalDateTime currentTime = LocalDateTime.now();
        notificationRepository.save(
                NotificationEntity.builder()
                        .phone(userId.toString())
                        .title(title)
                        .message(body)
                        .type(type)
                        .referenceId(String.valueOf(userId))
                        .read(false)
                        .pushed(true)
                        .createdAt(currentTime)
                        .build()
        );

        List<DeviceTokenEntity> devices =
                deviceTokenRepository.findByUserIdAndActiveTrue(userId);
        for (DeviceTokenEntity d : devices) {
            if (StringUtils.hasText(d.getFcmToken()) && fcmPushService.isAvailable()) {
                fcmPushService.send(d.getFcmToken(), title, body);
            } else if (StringUtils.hasText(d.getExpoPushToken())) {
                expoPushService.send(d.getExpoPushToken(), title, body);
            }
        }
    }
}
