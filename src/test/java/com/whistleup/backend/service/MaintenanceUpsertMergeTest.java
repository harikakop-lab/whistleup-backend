package com.whistleup.backend.service;

import com.whistleup.backend.resource.MaintenanceCreateResource;
import com.whistleup.backend.resource.MaintenanceMeterRowResource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceUpsertMergeTest {

    @Test
    void resolveEffectiveBase_secondSaveWaterOnly_preservesBaseFromExistingRow() {
        BigDecimal basePerFlat = BigDecimal.ZERO;
        boolean existingRow = true;
        BigDecimal oldAmount = new BigDecimal("1000.00");
        BigDecimal oldWater = BigDecimal.ZERO;
        BigDecimal oldAppliances = BigDecimal.ZERO;
        boolean blocks = MaintenanceUpsertMerge.blocksPreservedSharedBaseMerge(emptyExpenseRequest());

        BigDecimal effective = MaintenanceUpsertMerge.resolveEffectiveBase(
                basePerFlat,
                existingRow,
                oldAmount,
                oldWater,
                oldAppliances,
                blocks);

        assertThat(effective).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void blocksPreservedSharedBaseMerge_falseWhenAllExpenseFieldsAreZero() {
        MaintenanceCreateResource req = new MaintenanceCreateResource();
        req.setWatchmanSalary(BigDecimal.ZERO);
        req.setGarbageCollection(BigDecimal.ZERO);
        req.setLiftMaintenance(BigDecimal.ZERO);
        req.setElectricityCommon(BigDecimal.ZERO);
        req.setMotorPump(BigDecimal.ZERO);
        req.setMiscellaneous(BigDecimal.ZERO);
        assertThat(MaintenanceUpsertMerge.hasPositiveSharedExpenseSum(req)).isFalse();
        assertThat(MaintenanceUpsertMerge.blocksPreservedSharedBaseMerge(req)).isFalse();
    }

    @Test
    void blocksPreservedSharedBaseMerge_trueWhenResetFlag() {
        MaintenanceCreateResource req = emptyExpenseRequest();
        req.setResetSharedExpenses(true);
        assertThat(MaintenanceUpsertMerge.blocksPreservedSharedBaseMerge(req)).isTrue();
    }

    @Test
    void blocksPreservedSharedBaseMerge_trueWhenLegacyAmountPositive() {
        MaintenanceCreateResource req = emptyExpenseRequest();
        req.setAmount(new BigDecimal("1000"));
        assertThat(MaintenanceUpsertMerge.blocksPreservedSharedBaseMerge(req)).isTrue();
    }

    @Test
    void resolveWaterForFlat_preservesOldWaterWhenNoWaterPayload() {
        BigDecimal incoming = BigDecimal.ZERO;
        BigDecimal oldWater = new BigDecimal("50.00");
        boolean waterPayload = MaintenanceUpsertMerge.hasWaterPayload(new MaintenanceCreateResource());

        BigDecimal w = MaintenanceUpsertMerge.resolveWaterForFlat(incoming, true, oldWater, waterPayload);
        assertThat(w).isEqualByComparingTo(oldWater);
    }

    @Test
    void resolveWaterForFlat_usesIncomingWhenWaterPayloadPresent() {
        MaintenanceCreateResource req = new MaintenanceCreateResource();
        req.setWaterMode("FIXED");
        req.setFixedWaterBill(new BigDecimal("1000"));

        BigDecimal incoming = new BigDecimal("125.00");
        BigDecimal oldWater = new BigDecimal("50.00");
        boolean waterPayload = MaintenanceUpsertMerge.hasWaterPayload(req);

        BigDecimal w = MaintenanceUpsertMerge.resolveWaterForFlat(incoming, true, oldWater, waterPayload);
        assertThat(w).isEqualByComparingTo(incoming);
    }

    @Test
    void hasWaterPayload_falseWhenOnlyWaterModeWithoutBill() {
        MaintenanceCreateResource req = new MaintenanceCreateResource();
        req.setWaterMode("FIXED");
        assertThat(MaintenanceUpsertMerge.hasWaterPayload(req)).isFalse();
    }

    @Test
    void hasWaterPayload_trueWhenIndividualRateAndPositiveUnits() {
        MaintenanceCreateResource req = new MaintenanceCreateResource();
        req.setIndividualRatePerUnit(new BigDecimal("12"));
        req.setIndividualRows(List.of(MaintenanceMeterRowResource.builder()
                .flatNumber("101")
                .units(new BigDecimal("10"))
                .build()));
        assertThat(MaintenanceUpsertMerge.hasWaterPayload(req)).isTrue();
    }

    @Test
    void hasWaterPayload_falseWhenIndividualRateButNoPositiveUnits() {
        MaintenanceCreateResource req = new MaintenanceCreateResource();
        req.setIndividualRatePerUnit(new BigDecimal("12"));
        req.setIndividualRows(List.of(MaintenanceMeterRowResource.builder()
                .flatNumber("101")
                .units(BigDecimal.ZERO)
                .build()));
        assertThat(MaintenanceUpsertMerge.hasWaterPayload(req)).isFalse();
    }

    @Test
    void combinedBasePlusWater_matchesIncrementalTotal() {
        BigDecimal effectiveBase = MaintenanceUpsertMerge.resolveEffectiveBase(
                BigDecimal.ZERO,
                true,
                new BigDecimal("1000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false);
        BigDecimal water = new BigDecimal("125.00");
        assertThat(effectiveBase.add(water)).isEqualByComparingTo(new BigDecimal("1125.00"));
    }

    @Test
    void hasPositiveSharedExpenseSum_trueWhenAnyLinePositive() {
        MaintenanceCreateResource req = new MaintenanceCreateResource();
        req.setWatchmanSalary(BigDecimal.ZERO);
        req.setGarbageCollection(new BigDecimal("100.00"));
        assertThat(MaintenanceUpsertMerge.hasPositiveSharedExpenseSum(req)).isTrue();
        assertThat(MaintenanceUpsertMerge.blocksPreservedSharedBaseMerge(req)).isTrue();
    }

    @Test
    void hasPositiveSharedExpenseSum_trueForPositiveCustomExpense() {
        MaintenanceCreateResource req = new MaintenanceCreateResource();
        req.setCustomExpenses(Map.of("Repairs", new BigDecimal("50.00")));
        assertThat(MaintenanceUpsertMerge.hasPositiveSharedExpenseSum(req)).isTrue();
    }

    @Test
    void hasCustomExpensePayload_falseWhenMapEmptyOrAllNonPositive() {
        assertThat(MaintenanceUpsertMerge.hasCustomExpensePayload(new MaintenanceCreateResource())).isFalse();
        MaintenanceCreateResource req = new MaintenanceCreateResource();
        req.setCustomExpenses(Map.of("Repairs", BigDecimal.ZERO));
        assertThat(MaintenanceUpsertMerge.hasCustomExpensePayload(req)).isFalse();
    }

    @Test
    void hasCustomExpensePayload_trueWhenAnyCustomPositive() {
        MaintenanceCreateResource req = new MaintenanceCreateResource();
        req.setCustomExpenses(Map.of("Repairs", new BigDecimal("0.01")));
        assertThat(MaintenanceUpsertMerge.hasCustomExpensePayload(req)).isTrue();
    }

    private static MaintenanceCreateResource emptyExpenseRequest() {
        return new MaintenanceCreateResource();
    }
}
