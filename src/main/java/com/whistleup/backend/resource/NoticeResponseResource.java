package com.whistleup.backend.resource;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NoticeResponseResource {

    private UUID noticeId;
    private String title;
    private String description;
    private String type;
    private String profileId;
    private LocalDateTime createdAt;
}
