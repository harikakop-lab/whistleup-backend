package com.whistleup.backend.entity;

import com.whistleup.backend.constants.OrderStatus;
import com.whistleup.backend.constants.ServiceIssueStatus;
import com.whistleup.backend.constants.ServiceOrderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "order_id", updatable = false, nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private ServiceOrderType orderType;

    @Column(name = "profile_id", nullable = false)
    private String profileId;

    @Column(name = "building_id", nullable = false)
    private String buildingId;

    @Column(name = "service_city", nullable = true)
    private String serviceCity;

    @Column(name = "service_date", nullable = false)
    private LocalDate date;

    @Column(name = "service_time_slot")
    private String timeSlot;

    @Column(name = "option_id")
    private String optionId;

    @Column(name = "option_title")
    private String optionTitle;

    @Column(name = "notes")
    private String notes;

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "vhs_booking_id", length = 100)
    private String vhsBookingId;

    @Column(name = "vhs_status", length = 80)
    private String vhsStatus;

    @Column(name = "vhs_service_person_name", length = 160)
    private String vhsServicePersonName;

    @Column(name = "vhs_service_person_phone", length = 50)
    private String vhsServicePersonPhone;

    @Column(name = "order_creation_date", nullable = false, updatable = false)
    private LocalDate orderCreationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.CREATED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_person_id", referencedColumnName = "service_person_id")
    private ServicePerson servicePerson;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_status")
    private ServiceIssueStatus issueStatus;

    @Column(name = "issue_text")
    private String issueText;

    @Column(name = "issue_raised_at")
    private LocalDateTime issueRaisedAt;

    @PrePersist
    protected void onCreate() {
        this.orderCreationDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));
    }
}