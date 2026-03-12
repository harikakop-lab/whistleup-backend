package com.whistleup.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private String type; // EVENT, MAINTENANCE, BILL

    @Column(nullable = false)
    private String referenceId; // eventId / billId

    @Column(nullable = false)
    private boolean read;

    @Column(nullable = false)
    private boolean pushed;

    private LocalDateTime lastRemindedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}

