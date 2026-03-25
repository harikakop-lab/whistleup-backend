package com.whistleup.backend.service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.whistleup.backend.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmPushService {

    private final ObjectProvider<FirebaseMessaging> firebaseMessaging;
    private final DeviceTokenRepository deviceTokenRepository;

    public boolean isAvailable() {
        boolean ok = firebaseMessaging.getIfAvailable() != null;
        if (!ok) {
            log.debug("[push][fcm] isAvailable=false (FirebaseMessaging bean not registered)");
        }
        return ok;
    }

    public void send(String fcmToken, String title, String body) {
        send(fcmToken, title, body, Map.of());
    }

    public void send(String fcmToken, String title, String body, Map<String, String> data) {
        FirebaseMessaging fm = firebaseMessaging.getIfAvailable();
        if (fm == null) {
            log.warn("[push][fcm] skip send: FirebaseMessaging not configured (set FIREBASE_CREDENTIALS_PATH)");
            return;
        }
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("[push][fcm] skip send: empty FCM registration token");
            return;
        }
        String tokenPreview = fcmToken.length() > 20 ? fcmToken.substring(0, 20) + "…" : fcmToken;
        log.info("[push][fcm] sending title=\"{}\" to tokenPrefix={}", title, tokenPreview);
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .putAllData(data != null ? data : Map.of())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .putHeader("apns-priority", "10")
                            .setAps(Aps.builder().setSound("default").build())
                            .build())
                    .build();
            String messageId = fm.send(message);
            log.info("[push][fcm] success messageId={}", messageId);
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode code = e.getMessagingErrorCode();
            log.warn(
                    "[push][fcm] FAILED messagingErrorCode={} message={}",
                    code,
                    e.getMessage());
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                log.info("[push][fcm] deactivating stored token (invalid or unregistered)");
                deviceTokenRepository.deactivateByFcmToken(fcmToken);
            }
        } catch (Exception e) {
            log.error("[push][fcm] unexpected error", e);
        }
    }
}
