package com.whistleup.backend.resource;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class WaterBillRequest {

    @NotNull
    private Long buildingId;

    @NotNull
    private Integer year;

    @NotEmpty
    private String month;

    private Double liftCurrentBill;
    private Double commonCurrentBill;
    private Double garbage;
    private Double watchmanSalary;
    private Double miscellaneousExpenses;
    private Double liftMotorMaintenance;
    private Double otherExpenses;

    @Valid
    @NotEmpty
    private List<WaterReadingRequest> waterReadings;
}
