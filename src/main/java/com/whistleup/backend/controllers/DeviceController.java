package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.RegisterDeviceRequest;
import com.whistleup.backend.service.DeviceTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whistleup/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping("/register")
    public ResponseEntity<Void> registerDevice(@Valid @RequestBody RegisterDeviceRequest request) {
        Long userId = Long.valueOf(request.getPhone());
        deviceTokenService.registerDevice(userId, request.getExpoPushToken(), request.getPlatform());
        return ResponseEntity.ok().build();
    }
}