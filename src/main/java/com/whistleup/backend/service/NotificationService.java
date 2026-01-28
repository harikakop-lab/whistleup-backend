//package com.whistleup.backend.service;
//
//import com.whistleup.backend.entity.NotificationEntity;
//import com.whistleup.backend.repository.NotificationRepository;
//import com.whistleup.backend.repository.UserPushTokenRepository;
//import com.whistleup.backend.resource.NotificationResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//public class NotificationService {
//
//    private final NotificationRepository notificationRepo;
//    private final UserPushTokenRepository tokenRepo;
//    private final PushNotificationService pushService;
//
//    public void createAndPushNotification(
//            String phone,
//            String title,
//            String message,
//            String type,
//            String referenceId
//    ) {
//        NotificationEntity n = NotificationEntity.builder()
//                .phone(phone)
//                .title(title)
//                .message(message)
//                .type(type)
//                .referenceId(referenceId)
//                .read(false)
//                .pushed(false)
//                .createdAt(LocalDateTime.now())
//                .build();
//
//        notificationRepo.save(n);
//
//        tokenRepo.findById(phone).ifPresent(token -> {
//            pushService.sendPush(
//                    token.getPushToken(),
//                    title,
//                    message,
//                    Map.of(
//                        "type", type,
//                        "referenceId", referenceId
//                    )
//            );
//            n.setPushed(true);
//            notificationRepo.save(n);
//        });
//    }
//
//    public List<NotificationResponse> getNotifications(String phone) {
//        return notificationRepo.findByPhoneOrderByCreatedAtDesc(phone)
//                .stream()
//                .map(NotificationResponse::from)
//                .toList();
//    }
//
//    public void markAsRead(Long id) {
//        notificationRepo.findById(id).ifPresent(n -> {
//            n.setRead(true);
//            notificationRepo.save(n);
//        });
//    }
//}
