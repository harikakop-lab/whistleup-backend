package com.whistleup.backend.entity;

import com.whistleup.backend.constants.OrderStatus;
import com.whistleup.backend.constants.ServiceOrderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "service_orders")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", updatable = false, nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private ServiceOrderType orderType;

    @Column(name = "profile_id", nullable = false)
    private String profileId;

    @Column(name = "building_id", nullable = false)
    private String buildingId;

    @Column(name = "service_date", nullable = false)
    private LocalDate date;

    @Column(name = "order_creation_date", nullable = false, updatable = false)
    private LocalDate orderCreationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.CREATED;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_person_id", referencedColumnName = "service_person_id")
    private ServicePerson servicePerson;

    @PrePersist
    protected void onCreate() {
        this.orderCreationDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
    }
}