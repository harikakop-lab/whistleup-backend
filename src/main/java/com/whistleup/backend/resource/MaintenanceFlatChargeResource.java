package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MaintenanceFlatChargeResource {
    private String flatNumber;
    private BigDecimal amount;
    private BigDecimal baseAmount;
    private BigDecimal waterAmount;
    /** Optional per-flat appliance fee when UI sends variable amounts; summed by flat if duplicated. */
    private BigDecimal appliancesAmount;
}
