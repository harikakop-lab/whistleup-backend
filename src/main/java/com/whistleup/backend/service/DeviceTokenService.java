package com.whistleup.backend.service;

import com.whistleup.backend.entity.DeviceTokenEntity;
import com.whistleup.backend.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
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

        log.info(
                "[push][register] userId={} platform={} expoTokenPresent={} fcmTokenPresent={}",
                userId,
                platform,
                StringUtils.hasText(expoPushToken),
                StringUtils.hasText(fcmToken)
        );
        if (StringUtils.hasText(fcmToken)) {
            log.info("[push][register] fcmTokenPrefix={}", fcmToken.trim().substring(0, Math.min(12, fcmToken.trim().length())));
        }

        // Allow multiple devices per user. Update existing row for this specific device token when present.
        Optional<DeviceTokenEntity> existing = Optional.empty();
        if (StringUtils.hasText(fcmToken)) {
            existing = repo.findByFcmTokenAndUserId(fcmToken.trim(), userId);
        }
        if (existing.isEmpty() && StringUtils.hasText(expoPushToken)) {
            existing = repo.findByExpoPushTokenAndUserId(expoPushToken.trim(), userId);
        }

        DeviceTokenEntity entity = existing.orElseGet(DeviceTokenEntity::new);
        entity.setUserId(userId);
        entity.setExpoPushToken(StringUtils.hasText(expoPushToken) ? expoPushToken.trim() : entity.getExpoPushToken());
        entity.setFcmToken(StringUtils.hasText(fcmToken) ? fcmToken.trim() : entity.getFcmToken());
        entity.setPlatform(platform);
        entity.setActive(true);
        entity.setLastSeen(LocalDateTime.now());

        repo.save(entity);
    }
}
