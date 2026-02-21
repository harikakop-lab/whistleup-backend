package com.whistleup.backend.scheduler;

import com.whistleup.backend.service.NotificationSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationSendService notificationSendService;

    /**
     * Runs every 1 minute
     */
    @Scheduled(cron = "0 0 * * * *")
    public void sendMinuteNotifications() {
        log.info("⏰ Notification scheduler triggered at {}", LocalDateTime.now());
        // 🔹 TEMPORARY: hardcoded user IDs (replace later)
        List<Long> userIds = List.of(9666499643L);
        for (Long userId : userIds) {
            notificationSendService.notifyUser(
                    userId,
                    "⏱ Minute Reminder",
                    "This is an automated notification sent every minute.",
                    "SYSTEM"
            );
        }
    }
}