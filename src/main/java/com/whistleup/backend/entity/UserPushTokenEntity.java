package com.whistleup.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_push_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPushTokenEntity {

    @Id
    private String phone;

    @Column(nullable = false, unique = true)
    private String pushToken;

    @Column(nullable = false)
    private String platform; // android / ios
}
