package com.whistleup.backend.scheduler;

import com.whistleup.backend.constants.IssueType;
import com.whistleup.backend.entity.Maintenance;
import com.whistleup.backend.service.MaintenanceService;
import com.whistleup.backend.service.NotificationSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final ZoneId NOTIFICATION_ZONE = ZoneId.of("Asia/Kolkata");

    private final NotificationSendService notificationSendService;

    private final MaintenanceService maintenanceService;

    /**
     * Runs every 1 minute
     */
    @Scheduled(cron = "0 0 8,17 * * *", zone = "Asia/Kolkata")
    public void sendMinuteNotifications() {
        log.info("⏰ Notification scheduler triggered at {}", LocalDateTime.now());
        val pendingMaintenances = maintenanceService.getListOfPendingMaintenance();
        LocalDate today = LocalDate.now(NOTIFICATION_ZONE);
        pendingMaintenances.forEach(maintenance -> {
            String body = buildMaintenanceReminderBody(maintenance, today);
            notificationSendService.notifyUser(
                    Long.valueOf(maintenance.getProfileId()),
                    "💰Maintenance",
                    body,
                    IssueType.ALERT.name());
        });
    }

    private static String buildMaintenanceReminderBody(Maintenance maintenance, LocalDate today) {
        if (maintenance.getDueDate() != null && maintenance.getDueDate().isBefore(today)) {
            return "Please pay your maintenance of ₹" + maintenance.getAmount() + " for "
                    + getMonthStatic(maintenance.getMaintenanceMonth()) + " month by today.";
        }
        return "Please pay your maintenance of ₹" + maintenance.getAmount() + " for "
                + getMonthStatic(maintenance.getMaintenanceMonth()) + " month by " + maintenance.getDueDate() + ".";
    }

    private static String getMonthStatic(Integer maintenanceMonth) {
        if (maintenanceMonth == null) {
            return "current";
        }
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