package com.whistleup.backend.resource;

import com.whistleup.backend.entity.NotificationEntity;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private String title;
    private String message;
    private String type;
    private String referenceId;
    private boolean read;
    private LocalDateTime createdAt;

    public static NotificationResponse from(NotificationEntity n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .referenceId(n.getReferenceId())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
