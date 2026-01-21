package com.whistleup.backend.resource;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WaterReadingRequest {

    @NotEmpty
    private String flatNumber;

    @NotNull
    private Double meterReading;

    @NotNull
    private Double amount;
}
