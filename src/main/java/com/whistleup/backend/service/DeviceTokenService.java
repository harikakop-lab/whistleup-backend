package com.whistleup.backend.service;

import com.whistleup.backend.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository repo;

    @Transactional
    public void registerDevice(
            Long userId,
            String expoPushToken,
            String platform
    ) {
        repo.saveOrUpdate(userId, expoPushToken, platform);
    }
}