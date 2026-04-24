package com.whistleup.backend.service;

import com.whistleup.backend.constants.MaintenanceStatus;
import com.whistleup.backend.entity.Maintenance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceLedgerAggregationTest {

    @Test
    void canonicalTotalCollected_usesStoredFixedWhenAmountUnderreported() {
        Maintenance m = Maintenance.builder()
                .profileId("p1")
                .maintenanceYear(2026)
                .maintenanceMonth(4)
                .amount(new BigDecimal("125.00"))
                .fixedMaintenance(new BigDecimal("1000.00"))
                .waterAmount(new BigDecimal("125.00"))
                .appliancesAmount(BigDecimal.ZERO)
                .dueDate(LocalDate.of(2026, 4, 30))
                .status(MaintenanceStatus.PENDING)
                .buildingId("1")
                .build();

        assertThat(MaintenanceLedgerAggregation.canonicalTotalCollected(java.util.List.of(m)))
                .isEqualByComparingTo(new BigDecimal("1125.00"));
        assertThat(MaintenanceLedgerAggregation.fixedMaintenanceCollectionTotal(java.util.List.of(m)))
                .isEqualTo(1000.0);
    }

    @Test
    void canonicalTotalCollected_fallsBackToImpliedFixedWhenColumnNull() {
        Maintenance m = Maintenance.builder()
                .profileId("p1")
                .maintenanceYear(2026)
                .maintenanceMonth(4)
                .amount(new BigDecimal("1125.00"))
                .fixedMaintenance(null)
                .waterAmount(new BigDecimal("125.00"))
                .appliancesAmount(BigDecimal.ZERO)
                .dueDate(LocalDate.of(2026, 4, 30))
                .status(MaintenanceStatus.PENDING)
                .buildingId("1")
                .build();

        assertThat(MaintenanceLedgerAggregation.canonicalTotalCollected(java.util.List.of(m)))
                .isEqualByComparingTo(new BigDecimal("1125.00"));
    }

    @Test
    void fixedMaintenanceCollectionTotal_excludesCustomSoLedgerLinesSumToCanonical() {
        Map<String, BigDecimal> custom = new LinkedHashMap<>();
        custom.put("Repairs", new BigDecimal("50.00"));
        Maintenance m = Maintenance.builder()
                .profileId("p1")
                .maintenanceYear(2026)
                .maintenanceMonth(4)
                .amount(new BigDecimal("1175.00"))
                .fixedMaintenance(new BigDecimal("1050.00"))
                .waterAmount(new BigDecimal("125.00"))
                .appliancesAmount(BigDecimal.ZERO)
                .customExpenses(custom)
                .dueDate(LocalDate.of(2026, 4, 30))
                .status(MaintenanceStatus.PENDING)
                .buildingId("1")
                .build();

        assertThat(MaintenanceLedgerAggregation.canonicalTotalCollected(java.util.List.of(m)))
                .isEqualByComparingTo(new BigDecimal("1175.00"));
        assertThat(MaintenanceLedgerAggregation.fixedMaintenanceCollectionTotal(java.util.List.of(m)))
                .isEqualTo(1000.0);
        assertThat(MaintenanceLedgerAggregation.mergeCustomExpenseTotals(java.util.List.of(m)))
                .containsEntry("Repairs", new BigDecimal("50.00"));
    }

    @Test
    void canonicalTotalCollected_includesCustomWhenAmountReflectsFullPerFlatCharge() {
        Map<String, BigDecimal> custom = new LinkedHashMap<>();
        custom.put("Water tanker", new BigDecimal("625.00"));
        Maintenance m = Maintenance.builder()
                .profileId("p1")
                .maintenanceYear(2026)
                .maintenanceMonth(4)
                .amount(new BigDecimal("1625.00"))
                .fixedMaintenance(new BigDecimal("1000.00"))
                .waterAmount(BigDecimal.ZERO)
                .appliancesAmount(BigDecimal.ZERO)
                .customExpenses(custom)
                .dueDate(LocalDate.of(2026, 4, 30))
                .status(MaintenanceStatus.PENDING)
                .buildingId("1")
                .build();

        assertThat(MaintenanceLedgerAggregation.canonicalTotalCollected(java.util.List.of(m)))
                .isEqualByComparingTo(new BigDecimal("1625.00"));
        assertThat(MaintenanceLedgerAggregation.fixedMaintenanceCollectionTotal(java.util.List.of(m)))
                .isEqualTo(1000.0);
        assertThat(MaintenanceLedgerAggregation.mergeCustomExpenseTotals(java.util.List.of(m)))
                .containsEntry("Water tanker", new BigDecimal("625.00"));
    }
}
