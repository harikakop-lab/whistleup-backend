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
    private LocalDate dueDate;
    private MaintenanceStatus status;
    private LocalDate paidDate;
    private boolean invoiceAvailable;
}
