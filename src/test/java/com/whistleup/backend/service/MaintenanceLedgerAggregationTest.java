package com.whistleup.backend.service;

import com.whistleup.backend.constants.MaintenanceStatus;
import com.whistleup.backend.entity.Maintenance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

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
}
