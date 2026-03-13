package com.whistleup.backend.resource;

import com.whistleup.backend.constants.MaintenanceStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class MaintenanceResponseResource {
    private Long id;
    private String profileId;
    private String buildingId;
    private Integer year;
    private Integer month;
    private String monthLabel;     // "April Maintenance"
    private BigDecimal amount;
    private BigDecimal watchmanSalary;
    private BigDecimal garbageCollection;
    private BigDecimal liftMaintenance;
    private BigDecimal electricityCommon;
    private BigDecimal motorPump;
    private BigDecimal miscellaneous;
    private BigDecimal waterAmount;
    private String waterMode;
    private LocalDate dueDate;
    private MaintenanceStatus status;
    private LocalDate paidDate;
    private boolean invoiceAvailable;
}
