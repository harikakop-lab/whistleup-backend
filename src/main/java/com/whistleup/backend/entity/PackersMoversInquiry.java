package com.whistleup.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "packers_movers_inquiry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackersMoversInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_id", nullable = false)
    private String profileId;

    @Column(name = "building_id", nullable = false)
    private String buildingId;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "subcategory_key", nullable = false, length = 120)
    private String subcategoryKey;

    @Lob
    @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(name = "walkthrough_video_file_name", length = 255)
    private String walkthroughVideoFileName;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (status == null || status.isBlank()) {
            status = "NEW";
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
