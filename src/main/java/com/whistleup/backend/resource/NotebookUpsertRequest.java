package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotebookUpsertRequest {

    @NotBlank
    private String buildingId;

    @NotNull
    private Integer year;

    @NotBlank
    private String month;

    @NotNull
    private BigDecimal fixedMaintenance;

    @NotNull
    private Integer residentCount;

    @NotNull
    private BigDecimal waterBillAmount;

    @NotNull
    private Boolean usePreviousBalance;

    @NotNull
    private BigDecimal openingBalance;

    @NotNull
    private Map<String, BigDecimal> expenseBreakdown;
}
