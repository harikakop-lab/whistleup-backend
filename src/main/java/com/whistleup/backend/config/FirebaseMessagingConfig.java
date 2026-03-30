package com.whistleup.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
@Conditional(FirebaseCredentialsFilePresentCondition.class)
public class FirebaseMessagingConfig {

    @Bean
    public FirebaseMessaging firebaseMessaging(
            @Value("${firebase.credentials.path}") String credentialsPath
    ) throws IOException {
        try (InputStream in = new FileInputStream(credentialsPath.trim())) {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(in))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("[push] FirebaseApp initialized for FCM (FirebaseMessaging bean ready)");
            } else {
                log.info("[push] FirebaseApp already initialized; reusing existing app");
            }
            return FirebaseMessaging.getInstance();
        }
    }
}
