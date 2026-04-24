package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MaintenanceCreateResource {
    private String profileId;
    private Integer year;
    private Integer month;
    private BigDecimal amount;
    private BigDecimal fixedMaintenance;
    private LocalDate dueDate;
    private Integer totalFlats;

    // Shared expense inputs from maintenance UI (step 2)
    private BigDecimal watchmanSalary;
    private BigDecimal garbageCollection;
    private BigDecimal liftMaintenance;
    private BigDecimal electricityCommon;
    private BigDecimal motorPump;
    private BigDecimal miscellaneous;
    private Map<String, BigDecimal> customExpenses;

    // Water setup from maintenance UI (step 3)
    private String waterMode; // FIXED | MASTER | INDIVIDUAL | MIXED
    private BigDecimal fixedWaterBill;
    private BigDecimal masterWaterBill;
    private BigDecimal individualRatePerUnit;
    private BigDecimal mixedRatePerUnit;
    private BigDecimal mixedFixedPool;
    private List<MaintenanceMeterRowResource> individualRows;
    private List<MaintenanceMeterRowResource> mixedMeterRows;

    // Optional explicit per-flat totals from UI summary (step 4)
    private List<MaintenanceFlatChargeResource> flatCharges;

    // Optional full list of flats from UI for mixed setup calculations
    private List<String> allFlats;

    @NotNull
    private String buildingId;

    /** Total appliance maintenance for the month; split equally across opted-in residents when building flag is on. */
    private BigDecimal appliancesTotalAmount;

    /**
     * Optional per-resident appliance fee (when UI collects Fee/Month per opted-in phone). Keys are tenant phone
     * numbers. When non-empty, takes precedence over {@link MaintenanceFlatChargeResource#getAppliancesAmount()} for
     * those profiles.
     */
    private Map<String, BigDecimal> applianceFeesByPhone;
}
