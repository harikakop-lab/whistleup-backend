package com.whistleup.backend.resource;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {

    private Long id;
    private String title;
    private String body;
    private String type;
    private Boolean isRead;
    private LocalDateTime createdAt;
}