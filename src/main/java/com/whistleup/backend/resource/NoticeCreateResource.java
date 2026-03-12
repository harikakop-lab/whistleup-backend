package com.whistleup.backend.resource;

import com.whistleup.backend.constants.NoticeAudience;
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
    private NoticeAudience audience;
}
