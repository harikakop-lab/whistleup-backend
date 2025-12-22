package com.whistleup.backend.controllers;

import com.whistleup.backend.entity.UserPushTokenEntity;
import com.whistleup.backend.repository.UserPushTokenRepository;
import com.whistleup.backend.resource.NotificationResponse;
import com.whistleup.backend.resource.RegisterPushTokenRequest;
import com.whistleup.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserPushTokenRepository tokenRepo;

    @PostMapping("/register-token")
    public void registerToken(
            @RequestBody RegisterPushTokenRequest req
    ) {
        tokenRepo.save(
                new UserPushTokenEntity(
                        req.getPhone(),
                        req.getPushToken(),
                        req.getPlatform()
                )
        );
    }

    @GetMapping
    public List<NotificationResponse> getNotifications(@RequestParam String phone) {
        return notificationService.getNotifications(phone);
    }

    @PutMapping("/{id}/read")
    public void markRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
    }
}
