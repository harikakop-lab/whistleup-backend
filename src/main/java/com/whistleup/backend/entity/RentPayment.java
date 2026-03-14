package com.whistleup.backend.entity;

import com.whistleup.backend.constants.RentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "rent_payment",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"profile_id", "building_id", "rent_year", "rent_month"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_id", nullable = false)
    private String profileId;

    @Column(name = "building_id", nullable = false)
    private String buildingId;

    @Column(name = "rent_year", nullable = false)
    private Integer rentYear;

    @Column(name = "rent_month", nullable = false)
    private Integer rentMonth;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RentStatus status;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
