package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.RegisterDeviceRequest;
import com.whistleup.backend.service.DeviceTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whistleup/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping("/register")
    public ResponseEntity<Void> registerDevice(@Valid @RequestBody RegisterDeviceRequest request) {
        if (!StringUtils.hasText(request.getExpoPushToken()) && !StringUtils.hasText(request.getFcmToken())) {
            return ResponseEntity.badRequest().build();
        }
        Long userId = Long.valueOf(request.getPhone());
        deviceTokenService.registerDevice(
                userId,
                request.getExpoPushToken(),
                request.getFcmToken(),
                request.getPlatform());
        return ResponseEntity.ok().build();
    }
}