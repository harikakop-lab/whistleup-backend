package com.whistleup.backend.service;

import com.whistleup.backend.entity.DeviceTokenEntity;
import com.whistleup.backend.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository repo;

    @Transactional
    public void registerDevice(
            Long userId,
            String expoPushToken,
            String fcmToken,
            String platform
    ) {
        if (!StringUtils.hasText(expoPushToken) && !StringUtils.hasText(fcmToken)) {
            throw new IllegalArgumentException("At least one of expoPushToken or fcmToken is required");
        }

        repo.deleteAllByUserId(userId);

        DeviceTokenEntity entity = DeviceTokenEntity.builder()
                .userId(userId)
                .expoPushToken(StringUtils.hasText(expoPushToken) ? expoPushToken.trim() : null)
                .fcmToken(StringUtils.hasText(fcmToken) ? fcmToken.trim() : null)
                .platform(platform)
                .active(true)
                .lastSeen(LocalDateTime.now())
                .build();

        repo.save(entity);
    }
}
