package com.whistleup.backend.service;

import com.whistleup.backend.entity.NotificationEntity;
import com.whistleup.backend.repository.NotificationRepository;
import com.whistleup.backend.repository.UserPushTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class ReminderScheduler {

    private final NotificationRepository notificationRepo;
    private final UserPushTokenRepository tokenRepo;
    private final PushNotificationService pushService;

    @Scheduled(fixedRate = 12 * 60 * 60 * 1000)
    public void remindPendingMaintenance() {

        LocalDateTime threshold = LocalDateTime.now().minusHours(12);

        List<NotificationEntity> pending =
                notificationRepo.findPendingMaintenance(threshold);

        for (NotificationEntity n : pending) {
            tokenRepo.findById(n.getPhone()).ifPresent(token -> {
                pushService.sendPush(
                        token.getPushToken(),
                        n.getTitle(),
                        n.getMessage(),
                        Map.of(
                            "type", n.getType(),
                            "referenceId", n.getReferenceId()
                        )
                );
                n.setLastRemindedAt(LocalDateTime.now());
                notificationRepo.save(n);
            });
        }
    }
}
