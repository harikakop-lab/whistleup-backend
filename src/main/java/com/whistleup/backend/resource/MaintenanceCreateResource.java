package com.whistleup.backend.resource;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class MaintenanceCreateResource {
    private String profileId;
    private Integer year;
    private Integer month;
    private BigDecimal amount;
    private LocalDate dueDate;
}
