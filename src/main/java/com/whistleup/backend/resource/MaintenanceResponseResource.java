package com.whistleup.backend.resource;

import com.whistleup.backend.constants.MaintenanceStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

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
    private Map<String, BigDecimal> customExpenses;
    private BigDecimal waterAmount;
    private BigDecimal appliancesAmount;
    private String waterMode;
    private LocalDate dueDate;
    private MaintenanceStatus status;
    private LocalDate paidDate;
    private String paymentMethod;
    private String paymentReference;
    private String paymentProofUrl;
    private boolean invoiceAvailable;
}
