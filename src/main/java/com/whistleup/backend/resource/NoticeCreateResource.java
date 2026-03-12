package com.whistleup.backend.resource;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NoticeCreateResource {

    private String title;
    private String description;
    private String type;      // INFO / ALERT
    private String profileId;
    private String buildingId;
}
