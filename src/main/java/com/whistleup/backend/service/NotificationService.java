package com.whistleup.backend.service;

import com.whistleup.backend.notifications.entity.NotificationEntity;
import com.whistleup.backend.repository.NotificationRepository;
import com.whistleup.backend.resource.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long userId) {

        return repository
            .findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional
    public void saveNotification() {

    }

    private NotificationResponse mapToResponse(NotificationEntity n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .body(n.getBody())
                .type(n.getType())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}