package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.NotificationResponse;
import com.whistleup.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/whistleup/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 📬 Get notification inbox for logged-in user
     */
    @GetMapping("/{username}")
    public List<NotificationResponse> getMyNotifications(@PathVariable String username) {
        return notificationService.getUserNotifications(Long.valueOf(username));
    }
}