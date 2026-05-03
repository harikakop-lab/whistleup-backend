package com.whistleup.backend.service;

import com.whistleup.backend.constants.MaintenanceStatus;
import com.whistleup.backend.entity.Maintenance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    @Test
    void buildingAssetAmount_countsBuildingValueOnceAcrossRows() {
        Maintenance first = Maintenance.builder()
                .profileId("p1")
                .maintenanceYear(2026)
                .maintenanceMonth(4)
                .amount(new BigDecimal("1100.00"))
                .fixedMaintenance(new BigDecimal("1000.00"))
                .waterAmount(new BigDecimal("100.00"))
                .appliancesAmount(BigDecimal.ZERO)
                .assetAmount(new BigDecimal("3000.00"))
                .dueDate(LocalDate.of(2026, 4, 30))
                .status(MaintenanceStatus.PENDING)
                .buildingId("1")
                .build();
        Maintenance second = Maintenance.builder()
                .profileId("p2")
                .maintenanceYear(2026)
                .maintenanceMonth(4)
                .amount(new BigDecimal("1100.00"))
                .fixedMaintenance(new BigDecimal("1000.00"))
                .waterAmount(new BigDecimal("100.00"))
                .appliancesAmount(BigDecimal.ZERO)
                .assetAmount(new BigDecimal("3000.00"))
                .dueDate(LocalDate.of(2026, 4, 30))
                .status(MaintenanceStatus.PENDING)
                .buildingId("1")
                .build();

        assertThat(MaintenanceLedgerAggregation.buildingAssetAmount(List.of(first, second)))
                .isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(MaintenanceLedgerAggregation.canonicalTotalCollected(List.of(first, second)))
                .isEqualByComparingTo(new BigDecimal("5200.00"));
    }

    @Test
    void canonicalTotalCollected_withAccumulatedAsset_countsAssetOnlyOnceForBuilding() {
        Maintenance first = Maintenance.builder()
                .profileId("p1")
                .maintenanceYear(2026)
                .maintenanceMonth(5)
                .amount(new BigDecimal("1000.00"))
                .fixedMaintenance(new BigDecimal("1000.00"))
                .waterAmount(BigDecimal.ZERO)
                .appliancesAmount(BigDecimal.ZERO)
                .assetAmount(new BigDecimal("21000.00"))
                .status(MaintenanceStatus.PENDING)
                .buildingId("1")
                .build();
        Maintenance second = Maintenance.builder()
                .profileId("p2")
                .maintenanceYear(2026)
                .maintenanceMonth(5)
                .amount(new BigDecimal("1000.00"))
                .fixedMaintenance(new BigDecimal("1000.00"))
                .waterAmount(BigDecimal.ZERO)
                .appliancesAmount(BigDecimal.ZERO)
                .assetAmount(new BigDecimal("21000.00"))
                .status(MaintenanceStatus.PENDING)
                .buildingId("1")
                .build();

        assertThat(MaintenanceLedgerAggregation.canonicalTotalCollected(List.of(first, second)))
                .isEqualByComparingTo(new BigDecimal("23000.00"));
        assertThat(MaintenanceLedgerAggregation.sumAmounts(List.of(first, second)))
                .isEqualByComparingTo(new BigDecimal("2000.00"));
    }

    @Test
    void resolveBilledUnitCount_usesPersistedBilledUnitCount() {
        Maintenance m = Maintenance.builder()
                .profileId("p1")
                .maintenanceYear(2026)
                .maintenanceMonth(4)
                .amount(new BigDecimal("1000.00"))
                .fixedMaintenance(new BigDecimal("1000.00"))
                .waterAmount(BigDecimal.ZERO)
                .appliancesAmount(BigDecimal.ZERO)
                .dueDate(LocalDate.of(2026, 4, 30))
                .status(MaintenanceStatus.PENDING)
                .buildingId("1")
                .billedUnitCount(10)
                .build();

        assertThat(MaintenanceLedgerAggregation.resolveBilledUnitCount(List.of(m))).isEqualTo(10);
    }

    @Test
    void resolveBilledUnitCount_fallsBackToRowCountWhenColumnNull() {
        Maintenance a = Maintenance.builder()
                .profileId("p1")
                .maintenanceYear(2026)
                .maintenanceMonth(4)
                .amount(new BigDecimal("1000.00"))
                .fixedMaintenance(new BigDecimal("1000.00"))
                .waterAmount(BigDecimal.ZERO)
                .appliancesAmount(BigDecimal.ZERO)
                .dueDate(LocalDate.of(2026, 4, 30))
                .status(MaintenanceStatus.PENDING)
                .buildingId("1")
                .build();
        Maintenance b = Maintenance.builder()
                .profileId("p2")
                .maintenanceYear(2026)
                .maintenanceMonth(4)
                .amount(new BigDecimal("1000.00"))
                .fixedMaintenance(new BigDecimal("1000.00"))
                .waterAmount(BigDecimal.ZERO)
                .appliancesAmount(BigDecimal.ZERO)
                .dueDate(LocalDate.of(2026, 4, 30))
                .status(MaintenanceStatus.PENDING)
                .buildingId("1")
                .build();

        assertThat(MaintenanceLedgerAggregation.resolveBilledUnitCount(List.of(a, b))).isEqualTo(2);
    }

    @Test
    void resolveFixedPerFlat_dividesByBilledUnitCountNotPayerRowCount() {
        Maintenance row = Maintenance.builder()
                .profileId("p1")
                .maintenanceYear(2026)
                .maintenanceMonth(6)
                .amount(new BigDecimal("4500.00"))
                .fixedMaintenance(new BigDecimal("4500.00"))
                .waterAmount(BigDecimal.ZERO)
                .appliancesAmount(BigDecimal.ZERO)
                .dueDate(LocalDate.of(2026, 6, 30))
                .status(MaintenanceStatus.PENDING)
                .buildingId("1")
                .billedUnitCount(10)
                .build();

        assertThat(MaintenanceLedgerAggregation.resolveFixedPerFlat(List.of(row), 2, java.math.RoundingMode.HALF_UP))
                .isEqualByComparingTo(new BigDecimal("450.00"));
    }
}
