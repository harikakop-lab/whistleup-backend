package com.whistleup.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "device_tokens",
    indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_active", columnList = "active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL AUTO_INCREMENT
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expo_push_token", length = 512)
    private String expoPushToken;

    @Column(name = "fcm_token", length = 512)
    private String fcmToken;

    @Column(length = 20)
    private String platform; // ANDROID | IOS

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @UpdateTimestamp
    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    /* ----------------------------
       Helper methods
    ----------------------------- */

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public void touch() {
        this.lastSeen = LocalDateTime.now();
    }
}