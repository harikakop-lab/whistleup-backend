package com.whistleup.backend.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class NotebookResponse {

    private Long id;
    private String buildingId;
    private Integer year;
    private String month;
    private BigDecimal fixedMaintenance;
    private Integer residentCount;
    private BigDecimal waterBillAmount;
    private Boolean usePreviousBalance;
    private BigDecimal openingBalance;
    private BigDecimal collectionAmount;
    private BigDecimal totalBudget;
    private BigDecimal totalExpenses;
    private BigDecimal closingBalance;
    private Map<String, BigDecimal> expenseBreakdown;

    /** When true, fixed maintenance and water totals are sourced from maintenance rows for this month. */
    private Boolean maintenanceAnchored;
}
