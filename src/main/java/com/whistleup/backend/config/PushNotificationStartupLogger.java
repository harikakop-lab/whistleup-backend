package com.whistleup.backend.config;

import com.google.firebase.messaging.FirebaseMessaging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Logs whether server-side FCM (Firebase Admin) is active — helps diagnose "silent" skips.
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class PushNotificationStartupLogger implements ApplicationRunner {

    private final Environment environment;
    private final ObjectProvider<FirebaseMessaging> firebaseMessaging;

    @Override
    public void run(ApplicationArguments args) {
        String path = environment.getProperty("firebase.credentials.path", "");
        if (!StringUtils.hasText(path)) {
            log.warn(
                    "[push] FCM from server is DISABLED: firebase.credentials.path is empty. "
                            + "Set env FIREBASE_CREDENTIALS_PATH to your Firebase service account JSON path.");
            return;
        }
        Path p = Path.of(path.trim());
        if (!Files.isRegularFile(p)) {
            log.warn(
                    "[push] FCM from server is DISABLED: credentials file does not exist or is not a file: {}",
                    p.toAbsolutePath());
            return;
        }
        if (firebaseMessaging.getIfAvailable() != null) {
            log.info(
                    "[push] FCM from server is ENABLED. Using credentials file: {}",
                    p.toAbsolutePath());
        } else {
            log.warn(
                    "[push] FCM bean missing even though path is set. Check Firebase JSON and logs above for init errors.");
        }
    }
}
