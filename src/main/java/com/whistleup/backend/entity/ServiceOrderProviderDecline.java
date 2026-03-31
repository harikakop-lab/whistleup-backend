package com.whistleup.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(
        name = "service_order_provider_declines",
        uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "service_person_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceOrderProviderDecline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "service_person_id", nullable = false)
    private UUID servicePersonId;

    @Column(name = "declined_at", nullable = false, updatable = false)
    private LocalDateTime declinedAt;

    @PrePersist
    protected void onCreate() {
        this.declinedAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
    }
}

