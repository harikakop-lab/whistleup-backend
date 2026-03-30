package com.whistleup.backend.entity;

import com.whistleup.backend.constants.NoticeAudience;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notices")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Notice {

    @Id
    @GeneratedValue
    @Column(name = "notice_id", nullable = false, updatable = false)
    private UUID noticeId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "profile_id", nullable = false, length = 20)
    private String profileId;

    @Column(name = "building_id", nullable = false, length = 20)
    private String buildingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience", nullable = false, length = 30)
    private NoticeAudience audience;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
