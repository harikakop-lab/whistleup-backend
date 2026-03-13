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

    @Column
    private String phone;

    @Column
    private String title;

    @Column
    private String message;

    @Column
    private String type; // EVENT, MAINTENANCE, BILL

    @Column
    private String referenceId; // eventId / billId

    @Column(name = "is_read")
    private boolean read;

    @Column
    private boolean pushed;

    private LocalDateTime lastRemindedAt;

    @Column
    private LocalDateTime createdAt;
}

