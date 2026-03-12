package com.whistleup.backend.resource;

import com.whistleup.backend.constants.NoticeAudience;
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
    private NoticeAudience audience;
    private LocalDateTime createdAt;
}
