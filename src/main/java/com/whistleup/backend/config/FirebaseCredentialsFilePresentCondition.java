package com.whistleup.backend.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Enables Firebase when {@code firebase.credentials.path} points to an existing file.
 */
public class FirebaseCredentialsFilePresentCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String path = context.getEnvironment().getProperty("firebase.credentials.path", "");
        if (!StringUtils.hasText(path)) {
            return false;
        }
        return Files.isRegularFile(Path.of(path.trim()));
    }
}
