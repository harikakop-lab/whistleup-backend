package com.whistleup.backend.scheduler;

import com.whistleup.backend.constants.IssueType;
import com.whistleup.backend.service.MaintenanceService;
import com.whistleup.backend.service.NotificationSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationSendService notificationSendService;

    private final MaintenanceService maintenanceService;

    /**
     * Runs every 1 minute
     */
    @Scheduled(cron = "0 0 6,18 * * *", zone = "Asia/Kolkata")
    public void sendMinuteNotifications() {
        log.info("⏰ Notification scheduler triggered at {}", LocalDateTime.now());
        val pendingMaintenances = maintenanceService.getListOfPendingMaintenance();
        pendingMaintenances.forEach(maintenance -> {
            notificationSendService.notifyUser(
                    Long.valueOf(maintenance.getProfileId()),
                    "💰Maintenance",
                    "Please pay your maintenance of ₹" + maintenance.getAmount() + " for " + getMonth(maintenance.getMaintenanceMonth()) + "month.",
                    IssueType.ALERT.name()
            );
        });
    }

    private String getMonth(Integer maintenanceMonth) {
        return switch (maintenanceMonth) {
            case 1 -> "January";
            case 2 -> "February";
            case 3 -> "March";
            case 4 -> "April";
            case 5 -> "May";
            case 6 -> "June";
            case 7 -> "July";
            case 8 -> "August";
            case 9 -> "September";
            case 10 -> "October";
            case 11 -> "November";
            case 12 -> "December";
            default -> "current";
        };
    }
}