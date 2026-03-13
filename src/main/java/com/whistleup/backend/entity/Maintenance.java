package com.whistleup.backend.entity;

import com.whistleup.backend.constants.MaintenanceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "maintenance",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"profile_id", "maintenance_year", "maintenance_month"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Maintenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name ="profile_id", nullable = false)
    private String profileId;

    @Column(name = "maintenance_year", nullable = false)
    private Integer maintenanceYear;

    @Column(name = "maintenance_month", nullable = false)
    private Integer maintenanceMonth;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MaintenanceStatus status;

    @Column(name = "invoice_path")
    private String invoicePath;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "building_id")
    private String buildingId;

    // Per-flat expense breakdown persisted for ledger reconstruction.
    @Column(name = "watchman_salary", precision = 38, scale = 2)
    private BigDecimal watchmanSalary;

    @Column(name = "garbage_collection", precision = 38, scale = 2)
    private BigDecimal garbageCollection;

    @Column(name = "lift_maintenance", precision = 38, scale = 2)
    private BigDecimal liftMaintenance;

    @Column(name = "electricity_common", precision = 38, scale = 2)
    private BigDecimal electricityCommon;

    @Column(name = "motor_pump", precision = 38, scale = 2)
    private BigDecimal motorPump;

    @Column(name = "miscellaneous", precision = 38, scale = 2)
    private BigDecimal miscellaneous;

    @Column(name = "water_amount", precision = 38, scale = 2)
    private BigDecimal waterAmount;

    @Column(name = "water_mode", length = 20)
    private String waterMode;
}
