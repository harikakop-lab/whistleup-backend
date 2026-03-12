package com.whistleup.backend.resource;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventResponseResource {

    private String eventId;
    private String title;
    private String description;
    private String profileId;
    private String buildingId;
    private LocalDateTime createdAt;
}
